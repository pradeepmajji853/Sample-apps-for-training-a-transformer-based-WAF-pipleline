"""
LoRA-based Incremental Training for WAF Transformer
Parameter-efficient fine-tuning for continuous model updates
"""

import torch
import torch.nn as nn
from typing import Dict, List, Any, Optional
import logging
from pathlib import Path
import json
import time
from datetime import datetime

from peft import LoraConfig, get_peft_model, PeftModel, TaskType
from torch.utils.data import DataLoader
import numpy as np

from waf_model import WAFTransformer, WAFTokenizer
from trainer import LogSequenceDataset, collate_fn

class LoRATrainer:
    """LoRA-based incremental trainer for WAF model"""
    
    def __init__(
        self,
        base_model: WAFTransformer,
        tokenizer: WAFTokenizer,
        lora_rank: int = 16,
        lora_alpha: int = 32,
        lora_dropout: float = 0.1,
        target_modules: List[str] = None,
        learning_rate: float = 1e-4,
        device: str = 'cpu'
    ):
        self.base_model = base_model
        self.tokenizer = tokenizer
        self.device = device
        self.logger = logging.getLogger(__name__)
        
        # Default target modules for LoRA
        if target_modules is None:
            target_modules = ["query", "value", "key", "dense"]
            
        # LoRA configuration
        self.lora_config = LoraConfig(
            task_type=TaskType.FEATURE_EXTRACTION,
            r=lora_rank,
            lora_alpha=lora_alpha,
            lora_dropout=lora_dropout,
            target_modules=target_modules,
            bias="none"
        )
        
        # Create LoRA model
        self.lora_model = get_peft_model(base_model, self.lora_config)
        self.lora_model.to(device)
        
        # Optimizer for LoRA parameters only
        self.optimizer = torch.optim.AdamW(
            self.lora_model.parameters(),
            lr=learning_rate,
            weight_decay=0.01
        )
        
        # Loss function
        self.loss_fn = nn.CrossEntropyLoss(ignore_index=-100)
        
        # Training history
        self.training_history = []
        
    def incremental_update(
        self,
        new_sequences: List[List[str]],
        num_epochs: int = 3,
        batch_size: int = 16,
        replay_sequences: Optional[List[List[str]]] = None,
        replay_ratio: float = 0.3
    ) -> Dict[str, Any]:
        """
        Perform incremental update with new sequences
        
        Args:
            new_sequences: New benign sequences for training
            num_epochs: Number of training epochs
            batch_size: Batch size for training
            replay_sequences: Old sequences to replay (prevent catastrophic forgetting)
            replay_ratio: Ratio of replay sequences to include
        """
        start_time = time.time()
        self.logger.info(f"Starting incremental update with {len(new_sequences)} new sequences")
        
        # Prepare training data
        training_sequences = new_sequences.copy()
        
        # Add replay sequences to prevent catastrophic forgetting
        if replay_sequences and replay_ratio > 0:
            num_replay = int(len(new_sequences) * replay_ratio)
            if num_replay > 0:
                replay_sample = np.random.choice(
                    len(replay_sequences), 
                    min(num_replay, len(replay_sequences)), 
                    replace=False
                )
                training_sequences.extend([replay_sequences[i] for i in replay_sample])
                self.logger.info(f"Added {len(replay_sample)} replay sequences")
        
        # Create dataset and dataloader
        dataset = LogSequenceDataset(training_sequences, self.tokenizer, max_length=256)
        dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True, collate_fn=collate_fn)
        
        # Training loop
        self.lora_model.train()
        total_loss = 0.0
        num_batches = 0
        
        for epoch in range(num_epochs):
            epoch_loss = 0.0
            epoch_batches = 0
            
            for batch in dataloader:
                self.optimizer.zero_grad()
                
                # Move to device
                input_ids = batch['input_ids'].to(self.device)
                attention_mask = batch['attention_mask'].to(self.device)
                labels = batch['labels'].to(self.device)
                
                # Forward pass
                outputs = self.lora_model(
                    input_ids=input_ids,
                    attention_mask=attention_mask
                )
                
                # Compute MLM loss
                mlm_logits = outputs['mlm_logits']
                loss = self.loss_fn(
                    mlm_logits.view(-1, self.base_model.config.vocab_size),
                    labels.view(-1)
                )
                
                # Backward pass
                loss.backward()
                torch.nn.utils.clip_grad_norm_(self.lora_model.parameters(), max_norm=1.0)
                self.optimizer.step()
                
                epoch_loss += loss.item()
                epoch_batches += 1
                
            avg_epoch_loss = epoch_loss / epoch_batches if epoch_batches > 0 else 0.0
            self.logger.info(f"Epoch {epoch+1}/{num_epochs}, Loss: {avg_epoch_loss:.4f}")
            
            total_loss += epoch_loss
            num_batches += epoch_batches
            
        # Calculate metrics
        avg_loss = total_loss / num_batches if num_batches > 0 else 0.0
        training_time = time.time() - start_time
        
        # Update training history
        update_info = {
            'timestamp': datetime.utcnow().isoformat(),
            'num_new_sequences': len(new_sequences),
            'num_replay_sequences': len(training_sequences) - len(new_sequences),
            'num_epochs': num_epochs,
            'avg_loss': avg_loss,
            'training_time': training_time
        }
        self.training_history.append(update_info)
        
        self.logger.info(f"Incremental update completed in {training_time:.2f}s, avg loss: {avg_loss:.4f}")
        
        return update_info
        
    def merge_and_save(self, save_path: str):
        """
        Merge LoRA weights into base model and save
        """
        # Merge LoRA weights
        merged_model = self.lora_model.merge_and_unload()
        
        # Save merged model
        torch.save({
            'model_state_dict': merged_model.state_dict(),
            'model_config': self.base_model.config.__dict__,
            'tokenizer_vocab_size': self.tokenizer.vocab_size,
            'training_history': self.training_history
        }, save_path)
        
        # Save tokenizer
        tokenizer_path = save_path.replace('.pt', '_tokenizer.json')
        self.tokenizer.save_vocabulary(tokenizer_path)
        
        self.logger.info(f"Merged model saved to {save_path}")
        
    def save_lora_adapter(self, save_path: str):
        """
        Save only the LoRA adapter weights
        """
        self.lora_model.save_pretrained(save_path)
        self.logger.info(f"LoRA adapter saved to {save_path}")
        
    def load_lora_adapter(self, adapter_path: str):
        """
        Load LoRA adapter weights
        """
        self.lora_model = PeftModel.from_pretrained(self.base_model, adapter_path)
        self.lora_model.to(self.device)
        self.logger.info(f"LoRA adapter loaded from {adapter_path}")
        
    def get_trainable_parameters(self) -> Dict[str, int]:
        """
        Get count of trainable parameters
        """
        trainable_params = sum(p.numel() for p in self.lora_model.parameters() if p.requires_grad)
        total_params = sum(p.numel() for p in self.lora_model.parameters())
        
        return {
            'trainable_params': trainable_params,
            'total_params': total_params,
            'trainable_percentage': 100.0 * trainable_params / total_params
        }

class IncrementalUpdateManager:
    """
    Manager for handling incremental model updates in production
    """
    
    def __init__(
        self,
        model_path: str,
        tokenizer: WAFTokenizer,
        update_threshold: int = 1000,  # Number of samples before update
        max_replay_samples: int = 5000,
        device: str = 'cpu'
    ):
        self.model_path = model_path
        self.tokenizer = tokenizer
        self.update_threshold = update_threshold
        self.max_replay_samples = max_replay_samples
        self.device = device
        
        # Buffers for new data
        self.pending_sequences = []
        self.replay_buffer = []
        
        # Load base model
        self.base_model = self._load_base_model()
        
        # Initialize LoRA trainer
        self.lora_trainer = LoRATrainer(
            self.base_model,
            self.tokenizer,
            device=self.device
        )
        
        self.logger = logging.getLogger(__name__)
        
    def _load_base_model(self) -> WAFTransformer:
        """Load the base model"""
        if not Path(self.model_path).exists():
            # Create new model if not exists
            from waf_model import create_waf_model
            model, _ = create_waf_model()
            return model
            
        checkpoint = torch.load(self.model_path, map_location=self.device)
        
        from waf_model import WAFTransformerConfig
        config = WAFTransformerConfig(**checkpoint['model_config'])
        
        model = WAFTransformer(config)
        model.load_state_dict(checkpoint['model_state_dict'])
        
        return model.to(self.device)
        
    def add_benign_sequences(self, sequences: List[List[str]]):
        """
        Add new benign sequences to the update buffer
        """
        self.pending_sequences.extend(sequences)
        self.logger.info(f"Added {len(sequences)} sequences to update buffer. Total pending: {len(self.pending_sequences)}")
        
        # Check if we should trigger an update
        if len(self.pending_sequences) >= self.update_threshold:
            self._trigger_update()
            
    def _trigger_update(self):
        """
        Trigger an incremental model update
        """
        if not self.pending_sequences:
            return
            
        self.logger.info(f"Triggering incremental update with {len(self.pending_sequences)} sequences")
        
        # Perform incremental update
        update_info = self.lora_trainer.incremental_update(
            new_sequences=self.pending_sequences,
            replay_sequences=self.replay_buffer,
            num_epochs=3
        )
        
        # Update replay buffer (reservoir sampling)
        self._update_replay_buffer(self.pending_sequences)
        
        # Save updated model
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        updated_model_path = self.model_path.replace('.pt', f'_updated_{timestamp}.pt')
        self.lora_trainer.merge_and_save(updated_model_path)
        
        # Clear pending sequences
        self.pending_sequences.clear()
        
        self.logger.info(f"Incremental update completed. Updated model saved to {updated_model_path}")
        
        return update_info
        
    def _update_replay_buffer(self, new_sequences: List[List[str]]):
        """
        Update replay buffer using reservoir sampling
        """
        for sequence in new_sequences:
            if len(self.replay_buffer) < self.max_replay_samples:
                self.replay_buffer.append(sequence)
            else:
                # Reservoir sampling: replace random element
                idx = np.random.randint(0, len(self.replay_buffer))
                self.replay_buffer[idx] = sequence
                
    def force_update(self) -> Optional[Dict[str, Any]]:
        """
        Force an incremental update regardless of threshold
        """
        if self.pending_sequences:
            return self._trigger_update()
        return None
        
    def get_update_status(self) -> Dict[str, Any]:
        """
        Get current update status
        """
        return {
            'pending_sequences': len(self.pending_sequences),
            'replay_buffer_size': len(self.replay_buffer),
            'update_threshold': self.update_threshold,
            'ready_for_update': len(self.pending_sequences) >= self.update_threshold,
            'trainable_params': self.lora_trainer.get_trainable_parameters()
        }

if __name__ == "__main__":
    # Test LoRA training
    logging.basicConfig(level=logging.INFO)
    
    from waf_model import create_waf_model
    
    # Create base model
    model, tokenizer = create_waf_model(vocab_size=1000)
    
    # Add sample vocabulary
    sample_tokens = ['GET', 'POST', 'PUT', 'DELETE', '/api', '/users', '/posts', '200', '404', '500']
    for token in sample_tokens:
        tokenizer.add_token(token)
    
    # Sample sequences
    sample_sequences = [
        ['GET', '/api/users', '200'],
        ['POST', '/api/users', '201'],
        ['GET', '/api/posts', '200'],
        ['DELETE', '/api/users', '204'],
    ] * 50  # Repeat for more data
    
    # Test LoRA trainer
    lora_trainer = LoRATrainer(model, tokenizer, device='cpu')
    
    print("Trainable parameters:", lora_trainer.get_trainable_parameters())
    
    # Perform incremental update
    update_info = lora_trainer.incremental_update(sample_sequences, num_epochs=2)
    print("Update info:", update_info)
    
    # Test incremental update manager
    manager = IncrementalUpdateManager("test_model.pt", tokenizer, update_threshold=10)
    
    # Add sequences
    manager.add_benign_sequences(sample_sequences[:5])
    print("Status:", manager.get_update_status())
    
    # Add more sequences to trigger update
    manager.add_benign_sequences(sample_sequences[5:15])
    print("Status after update:", manager.get_update_status())
    
    print("LoRA incremental training test completed!")
