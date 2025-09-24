"""
Training Pipeline for WAF Transformer
Implements LogBERT-style training with masked language modeling and contrastive learning
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader, random_split
from typing import Dict, List, Any, Optional, Tuple
import numpy as np
import json
import pickle
import logging
from pathlib import Path
import time
from datetime import datetime
import random
from tqdm import tqdm
from sklearn.metrics import roc_auc_score, precision_recall_fscore_support

from waf_model import WAFTransformer, WAFTokenizer, ContrastiveLoss, HypersphereLoss, create_waf_model

class LogSequenceDataset(Dataset):
    """Dataset for log sequences"""
    
    def __init__(
        self,
        sequences: List[List[str]],
        tokenizer: WAFTokenizer,
        max_length: int = 512,
        mlm_probability: float = 0.15
    ):
        self.sequences = sequences
        self.tokenizer = tokenizer
        self.max_length = max_length
        self.mlm_probability = mlm_probability
        
    def __len__(self):
        return len(self.sequences)
        
    def __getitem__(self, idx):
        sequence = self.sequences[idx]
        
        # Tokenize sequence
        encoded = self.tokenizer.encode(sequence, max_length=self.max_length)
        
        # Create masked LM labels
        input_ids = encoded['input_ids'].clone()
        labels = input_ids.clone()
        
        # Mask tokens for MLM
        for i in range(1, len(input_ids) - 1):  # Skip CLS and SEP tokens
            if torch.rand(1).item() < self.mlm_probability:
                # 80% replace with MASK, 10% replace with random, 10% keep original
                prob = torch.rand(1).item()
                if prob < 0.8:
                    input_ids[i] = self.tokenizer.special_tokens['[MASK]']
                elif prob < 0.9:
                    input_ids[i] = torch.randint(5, self.tokenizer.vocab_size, (1,)).item()
                # else keep original
            else:
                labels[i] = -100  # Ignore in loss calculation
                
        return {
            'input_ids': input_ids,
            'attention_mask': encoded['attention_mask'],
            'labels': labels,
            'original_sequence': sequence
        }

class WAFTrainer:
    """Trainer for WAF Transformer model"""
    
    def __init__(
        self,
        model: WAFTransformer,
        tokenizer: WAFTokenizer,
        device: str = 'cpu',
        learning_rate: float = 5e-5,
        weight_decay: float = 0.01,
        mlm_weight: float = 1.0,
        contrastive_weight: float = 0.1,
        hypersphere_weight: float = 0.1
    ):
        self.model = model.to(device)
        self.tokenizer = tokenizer
        self.device = device
        
        # Loss weights
        self.mlm_weight = mlm_weight
        self.contrastive_weight = contrastive_weight
        self.hypersphere_weight = hypersphere_weight
        
        # Optimizer
        self.optimizer = optim.AdamW(
            self.model.parameters(),
            lr=learning_rate,
            weight_decay=weight_decay
        )
        
        # Loss functions
        self.mlm_loss_fn = nn.CrossEntropyLoss(ignore_index=-100)
        self.contrastive_loss_fn = ContrastiveLoss()
        self.hypersphere_loss_fn = HypersphereLoss()
        
        # Metrics
        self.training_history = []
        self.logger = logging.getLogger(__name__)
        
    def train_epoch(self, dataloader: DataLoader) -> Dict[str, float]:
        """Train one epoch"""
        self.model.train()
        total_loss = 0.0
        mlm_loss_total = 0.0
        contrastive_loss_total = 0.0
        hypersphere_loss_total = 0.0
        num_batches = 0
        
        progress_bar = tqdm(dataloader, desc="Training")
        
        for batch in progress_bar:
            self.optimizer.zero_grad()
            
            # Move to device
            input_ids = batch['input_ids'].to(self.device)
            attention_mask = batch['attention_mask'].to(self.device)
            labels = batch['labels'].to(self.device)
            
            # Forward pass
            outputs = self.model(
                input_ids=input_ids,
                attention_mask=attention_mask
            )
            
            # Compute losses
            # MLM Loss
            mlm_logits = outputs['mlm_logits']
            mlm_loss = self.mlm_loss_fn(
                mlm_logits.view(-1, self.model.config.vocab_size),
                labels.view(-1)
            )
            
            # Contrastive Loss
            contrastive_repr = outputs['contrastive_repr']
            contrastive_loss = self.contrastive_loss_fn(contrastive_repr)
            
            # Hypersphere Loss
            hypersphere_loss = self.hypersphere_loss_fn(contrastive_repr)
            
            # Total loss
            total_batch_loss = (
                self.mlm_weight * mlm_loss +
                self.contrastive_weight * contrastive_loss +
                self.hypersphere_weight * hypersphere_loss
            )
            
            # Backward pass
            total_batch_loss.backward()
            
            # Gradient clipping
            torch.nn.utils.clip_grad_norm_(self.model.parameters(), max_norm=1.0)
            
            self.optimizer.step()
            
            # Update metrics
            total_loss += total_batch_loss.item()
            mlm_loss_total += mlm_loss.item()
            contrastive_loss_total += contrastive_loss.item()
            hypersphere_loss_total += hypersphere_loss.item()
            num_batches += 1
            
            # Update progress bar
            progress_bar.set_postfix({
                'loss': total_batch_loss.item(),
                'mlm': mlm_loss.item(),
                'cont': contrastive_loss.item(),
                'hyper': hypersphere_loss.item()
            })
            
        return {
            'total_loss': total_loss / num_batches,
            'mlm_loss': mlm_loss_total / num_batches,
            'contrastive_loss': contrastive_loss_total / num_batches,
            'hypersphere_loss': hypersphere_loss_total / num_batches
        }
        
    def evaluate(self, dataloader: DataLoader) -> Dict[str, float]:
        """Evaluate model"""
        self.model.eval()
        total_loss = 0.0
        mlm_loss_total = 0.0
        num_batches = 0
        
        all_anomaly_scores = []
        all_perplexities = []
        
        with torch.no_grad():
            for batch in tqdm(dataloader, desc="Evaluating"):
                # Move to device
                input_ids = batch['input_ids'].to(self.device)
                attention_mask = batch['attention_mask'].to(self.device)
                labels = batch['labels'].to(self.device)
                
                # Forward pass
                outputs = self.model(
                    input_ids=input_ids,
                    attention_mask=attention_mask
                )
                
                # Compute MLM loss
                mlm_logits = outputs['mlm_logits']
                mlm_loss = self.mlm_loss_fn(
                    mlm_logits.view(-1, self.model.config.vocab_size),
                    labels.view(-1)
                )
                
                total_loss += mlm_loss.item()
                mlm_loss_total += mlm_loss.item()
                num_batches += 1
                
                # Collect anomaly scores and perplexities
                anomaly_scores = outputs['anomaly_score'].cpu().numpy()
                all_anomaly_scores.extend(anomaly_scores)
                
                # Calculate perplexity for each sequence
                for i in range(input_ids.size(0)):
                    # Get non-masked tokens
                    seq_labels = labels[i]
                    valid_positions = seq_labels != -100
                    
                    if valid_positions.sum() > 0:
                        seq_logits = mlm_logits[i][valid_positions]
                        seq_labels_valid = seq_labels[valid_positions]
                        
                        # Compute perplexity
                        seq_loss = nn.CrossEntropyLoss()(seq_logits, seq_labels_valid)
                        perplexity = torch.exp(seq_loss).item()
                        all_perplexities.append(perplexity)
                        
        return {
            'total_loss': total_loss / num_batches,
            'mlm_loss': mlm_loss_total / num_batches,
            'perplexity': np.mean(all_perplexities) if all_perplexities else 0.0,
            'avg_anomaly_score': np.mean(all_anomaly_scores) if all_anomaly_scores else 0.0,
            'std_anomaly_score': np.std(all_anomaly_scores) if all_anomaly_scores else 0.0
        }
        
    def train(
        self,
        train_dataloader: DataLoader,
        val_dataloader: Optional[DataLoader] = None,
        num_epochs: int = 10,
        save_dir: str = "models",
        early_stopping_patience: int = 3
    ):
        """Full training loop"""
        save_path = Path(save_dir)
        save_path.mkdir(exist_ok=True)
        
        best_val_loss = float('inf')
        patience_counter = 0
        
        for epoch in range(num_epochs):
            start_time = time.time()
            
            # Training
            train_metrics = self.train_epoch(train_dataloader)
            
            # Validation
            val_metrics = {}
            if val_dataloader:
                val_metrics = self.evaluate(val_dataloader)
                
            epoch_time = time.time() - start_time
            
            # Logging
            log_msg = f"Epoch {epoch+1}/{num_epochs} ({epoch_time:.2f}s)"
            log_msg += f" - Train Loss: {train_metrics['total_loss']:.4f}"
            
            if val_metrics:
                log_msg += f" - Val Loss: {val_metrics['total_loss']:.4f}"
                log_msg += f" - Val Perplexity: {val_metrics['perplexity']:.2f}"
                
            self.logger.info(log_msg)
            print(log_msg)
            
            # Save metrics
            epoch_data = {
                'epoch': epoch + 1,
                'timestamp': datetime.now().isoformat(),
                'train_metrics': train_metrics,
                'val_metrics': val_metrics,
                'epoch_time': epoch_time
            }
            self.training_history.append(epoch_data)
            
            # Early stopping and model saving
            current_val_loss = val_metrics.get('total_loss', train_metrics['total_loss'])
            
            if current_val_loss < best_val_loss:
                best_val_loss = current_val_loss
                patience_counter = 0
                
                # Save best model
                self.save_model(save_path / "best_model.pt")
                self.save_training_history(save_path / "training_history.json")
                
                self.logger.info(f"New best model saved with validation loss: {best_val_loss:.4f}")
                
            else:
                patience_counter += 1
                if patience_counter >= early_stopping_patience and val_dataloader:
                    self.logger.info(f"Early stopping triggered after {epoch+1} epochs")
                    break
                    
            # Save checkpoint every few epochs
            if (epoch + 1) % 5 == 0:
                self.save_model(save_path / f"checkpoint_epoch_{epoch+1}.pt")
                
        # Save final model
        self.save_model(save_path / "final_model.pt")
        self.save_training_history(save_path / "final_training_history.json")
        
        self.logger.info("Training completed!")
        
    def save_model(self, path: str):
        """Save model and tokenizer"""
        torch.save({
            'model_state_dict': self.model.state_dict(),
            'optimizer_state_dict': self.optimizer.state_dict(),
            'model_config': self.model.config.__dict__,
            'tokenizer_vocab_size': self.tokenizer.vocab_size
        }, path)
        
        # Save tokenizer vocabulary
        tokenizer_path = str(path).replace('.pt', '_tokenizer.json')
        self.tokenizer.save_vocabulary(tokenizer_path)
        
    def load_model(self, path: str):
        """Load model and tokenizer"""
        checkpoint = torch.load(path, map_location=self.device)
        self.model.load_state_dict(checkpoint['model_state_dict'])
        self.optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
        
        # Load tokenizer vocabulary
        tokenizer_path = str(path).replace('.pt', '_tokenizer.json')
        if Path(tokenizer_path).exists():
            self.tokenizer.load_vocabulary(tokenizer_path)
            
    def save_training_history(self, path: str):
        """Save training history"""
        with open(path, 'w') as f:
            json.dump(self.training_history, f, indent=2)

def prepare_training_data(log_sequences: List[List[str]], tokenizer: WAFTokenizer) -> Tuple[Dataset, Dataset]:
    """Prepare training and validation datasets"""
    # Build vocabulary from sequences
    all_tokens = set()
    for sequence in log_sequences:
        all_tokens.update(sequence)
        
    # Add tokens to tokenizer
    for token in all_tokens:
        tokenizer.add_token(token)
        
    # Create datasets
    full_dataset = LogSequenceDataset(log_sequences, tokenizer)
    
    # Split into train/val
    train_size = int(0.8 * len(full_dataset))
    val_size = len(full_dataset) - train_size
    
    train_dataset, val_dataset = random_split(full_dataset, [train_size, val_size])
    
    return train_dataset, val_dataset

def collate_fn(batch):
    """Custom collate function for DataLoader"""
    input_ids = torch.stack([item['input_ids'] for item in batch])
    attention_mask = torch.stack([item['attention_mask'] for item in batch])
    labels = torch.stack([item['labels'] for item in batch])
    
    return {
        'input_ids': input_ids,
        'attention_mask': attention_mask,
        'labels': labels
    }

if __name__ == "__main__":
    # Test training pipeline
    logging.basicConfig(level=logging.INFO)
    
    # Create sample data
    sample_sequences = [
        ['GET', '/api/users', '200', 'Mozilla/5.0'],
        ['POST', '/api/users', '201', 'curl/7.68.0'],
        ['GET', '/blog/posts', '200', 'Mozilla/5.0'],
        ['DELETE', '/api/users/123', '204', 'curl/7.68.0'],
        ['GET', '/ecommerce/cart', '200', 'Mozilla/5.0'],
    ] * 100  # Repeat to have enough data
    
    # Create model and tokenizer
    model, tokenizer = create_waf_model(vocab_size=5000)
    
    # Prepare datasets
    train_dataset, val_dataset = prepare_training_data(sample_sequences, tokenizer)
    
    # Create dataloaders
    train_dataloader = DataLoader(train_dataset, batch_size=16, shuffle=True, collate_fn=collate_fn)
    val_dataloader = DataLoader(val_dataset, batch_size=16, shuffle=False, collate_fn=collate_fn)
    
    # Create trainer
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    trainer = WAFTrainer(model, tokenizer, device=device)
    
    # Train model
    trainer.train(
        train_dataloader=train_dataloader,
        val_dataloader=val_dataloader,
        num_epochs=5,
        save_dir="test_models"
    )
    
    print("Training completed successfully!")
