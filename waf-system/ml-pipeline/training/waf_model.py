"""
WAF Transformer Model
LogBERT-style transformer encoder for HTTP log anomaly detection
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
from transformers import BertConfig, BertModel, BertTokenizer
from transformers import PreTrainedModel, PreTrainedTokenizer
from typing import Dict, List, Optional, Tuple, Any
import numpy as np
import json
import pickle
from pathlib import Path

class WAFTokenizer:
    """Custom tokenizer for HTTP log sequences"""
    
    def __init__(self, vocab_size: int = 10000):
        self.vocab_size = vocab_size
        self.token_to_id = {}
        self.id_to_token = {}
        self.special_tokens = {
            '[PAD]': 0,
            '[CLS]': 1,
            '[SEP]': 2,
            '[MASK]': 3,
            '[UNK]': 4,
        }
        
        # Initialize with special tokens
        for token, idx in self.special_tokens.items():
            self.token_to_id[token] = idx
            self.id_to_token[idx] = token
            
        self.next_id = len(self.special_tokens)
        
    def add_token(self, token: str) -> int:
        """Add a new token to vocabulary"""
        if token not in self.token_to_id:
            if self.next_id >= self.vocab_size:
                return self.special_tokens['[UNK]']
                
            self.token_to_id[token] = self.next_id
            self.id_to_token[self.next_id] = token
            self.next_id += 1
            
        return self.token_to_id[token]
        
    def encode(self, tokens: List[str], max_length: int = 512) -> Dict[str, torch.Tensor]:
        """Encode tokens to tensor"""
        # Add CLS token at the beginning
        tokens = ['[CLS]'] + tokens[:max_length-2] + ['[SEP]']
        
        # Convert tokens to IDs
        input_ids = [self.token_to_id.get(token, self.special_tokens['[UNK]']) for token in tokens]
        
        # Pad sequence
        while len(input_ids) < max_length:
            input_ids.append(self.special_tokens['[PAD]'])
            
        input_ids = input_ids[:max_length]
        
        # Create attention mask
        attention_mask = [1 if id != self.special_tokens['[PAD]'] else 0 for id in input_ids]
        
        return {
            'input_ids': torch.tensor(input_ids, dtype=torch.long),
            'attention_mask': torch.tensor(attention_mask, dtype=torch.long)
        }
        
    def decode(self, input_ids: torch.Tensor) -> List[str]:
        """Decode tensor to tokens"""
        if input_ids.dim() > 1:
            input_ids = input_ids.squeeze()
            
        tokens = [self.id_to_token.get(id.item(), '[UNK]') for id in input_ids]
        return tokens
        
    def save_vocabulary(self, path: str):
        """Save vocabulary to file"""
        vocab_data = {
            'token_to_id': self.token_to_id,
            'id_to_token': self.id_to_token,
            'vocab_size': self.vocab_size,
            'next_id': self.next_id
        }
        with open(path, 'w') as f:
            json.dump(vocab_data, f, indent=2)
            
    def load_vocabulary(self, path: str):
        """Load vocabulary from file"""
        with open(path, 'r') as f:
            vocab_data = json.load(f)
            
        self.token_to_id = vocab_data['token_to_id']
        # Convert string keys back to integers for id_to_token
        self.id_to_token = {int(k): v for k, v in vocab_data['id_to_token'].items()}
        self.vocab_size = vocab_data['vocab_size']
        self.next_id = vocab_data['next_id']

class WAFTransformerConfig:
    """Configuration for WAF Transformer model"""
    
    def __init__(
        self,
        vocab_size: int = 10000,
        hidden_size: int = 256,
        num_hidden_layers: int = 4,
        num_attention_heads: int = 8,
        intermediate_size: int = 1024,
        max_position_embeddings: int = 512,
        dropout_prob: float = 0.1,
        layer_norm_eps: float = 1e-12,
        **kwargs
    ):
        self.vocab_size = vocab_size
        self.hidden_size = hidden_size
        self.num_hidden_layers = num_hidden_layers
        self.num_attention_heads = num_attention_heads
        self.intermediate_size = intermediate_size
        self.max_position_embeddings = max_position_embeddings
        self.dropout_prob = dropout_prob
        self.layer_norm_eps = layer_norm_eps

class WAFTransformer(nn.Module):
    """WAF Transformer model for log anomaly detection"""
    
    def __init__(self, config: WAFTransformerConfig):
        super().__init__()
        self.config = config
        
        # Token embeddings
        self.embeddings = nn.Embedding(config.vocab_size, config.hidden_size, padding_idx=0)
        self.position_embeddings = nn.Embedding(config.max_position_embeddings, config.hidden_size)
        
        # Transformer layers
        self.transformer_layers = nn.ModuleList([
            nn.TransformerEncoderLayer(
                d_model=config.hidden_size,
                nhead=config.num_attention_heads,
                dim_feedforward=config.intermediate_size,
                dropout=config.dropout_prob,
                batch_first=True
            )
            for _ in range(config.num_hidden_layers)
        ])
        
        self.layer_norm = nn.LayerNorm(config.hidden_size, eps=config.layer_norm_eps)
        self.dropout = nn.Dropout(config.dropout_prob)
        
        # Masked Language Model head
        self.mlm_head = nn.Linear(config.hidden_size, config.vocab_size)
        
        # Anomaly detection head (binary classification)
        self.anomaly_head = nn.Sequential(
            nn.Linear(config.hidden_size, config.hidden_size // 2),
            nn.ReLU(),
            nn.Dropout(config.dropout_prob),
            nn.Linear(config.hidden_size // 2, 1),
            nn.Sigmoid()
        )
        
        # Contrastive learning for normal behavior modeling
        self.contrastive_head = nn.Linear(config.hidden_size, config.hidden_size)
        
        self.init_weights()
        
    def init_weights(self):
        """Initialize model weights"""
        for module in self.modules():
            if isinstance(module, nn.Linear):
                module.weight.data.normal_(mean=0.0, std=0.02)
                if module.bias is not None:
                    module.bias.data.zero_()
            elif isinstance(module, nn.Embedding):
                module.weight.data.normal_(mean=0.0, std=0.02)
                if module.padding_idx is not None:
                    module.weight.data[module.padding_idx].zero_()
            elif isinstance(module, nn.LayerNorm):
                module.bias.data.zero_()
                module.weight.data.fill_(1.0)
                
    def forward(
        self,
        input_ids: torch.Tensor,
        attention_mask: Optional[torch.Tensor] = None,
        labels: Optional[torch.Tensor] = None,
        return_embeddings: bool = False
    ) -> Dict[str, torch.Tensor]:
        
        batch_size, seq_length = input_ids.shape
        
        # Create position IDs
        position_ids = torch.arange(seq_length, dtype=torch.long, device=input_ids.device)
        position_ids = position_ids.unsqueeze(0).expand(batch_size, -1)
        
        # Embeddings
        token_embeds = self.embeddings(input_ids)
        position_embeds = self.position_embeddings(position_ids)
        embeddings = token_embeds + position_embeds
        embeddings = self.layer_norm(embeddings)
        embeddings = self.dropout(embeddings)
        
        # Create attention mask for transformer
        if attention_mask is not None:
            # Convert to transformer format (True for positions to mask)
            extended_attention_mask = attention_mask.unsqueeze(1).unsqueeze(2)
            extended_attention_mask = (1.0 - extended_attention_mask) * -10000.0
        else:
            extended_attention_mask = None
            
        # Pass through transformer layers
        hidden_states = embeddings
        for layer in self.transformer_layers:
            hidden_states = layer(
                hidden_states,
                src_key_padding_mask=~attention_mask.bool() if attention_mask is not None else None
            )
            
        # Get sequence representation (CLS token)
        sequence_output = hidden_states[:, 0, :]  # CLS token
        pooled_output = self.layer_norm(sequence_output)
        
        outputs = {}
        
        # Masked Language Model predictions
        mlm_predictions = self.mlm_head(hidden_states)
        outputs['mlm_logits'] = mlm_predictions
        
        # Anomaly detection
        anomaly_score = self.anomaly_head(pooled_output).squeeze(-1)
        outputs['anomaly_score'] = anomaly_score
        
        # Contrastive representation
        contrastive_repr = self.contrastive_head(pooled_output)
        outputs['contrastive_repr'] = contrastive_repr
        
        if return_embeddings:
            outputs['hidden_states'] = hidden_states
            outputs['pooled_output'] = pooled_output
            
        return outputs

class ContrastiveLoss(nn.Module):
    """Contrastive loss for learning normal behavior representation"""
    
    def __init__(self, temperature: float = 0.1, margin: float = 1.0):
        super().__init__()
        self.temperature = temperature
        self.margin = margin
        
    def forward(self, embeddings: torch.Tensor) -> torch.Tensor:
        """
        NT-Xent-style loss over the batch using self as the positive target.
        embeddings: (batch_size, hidden_dim)
        """
        # Normalize embeddings
        embeddings = F.normalize(embeddings, p=2, dim=1)
        
        # Pairwise similarities (logits)
        logits = torch.matmul(embeddings, embeddings.t()) / self.temperature  # (B, B)
        
        # Targets are the diagonal (each sample treated as its own class)
        targets = torch.arange(logits.size(0), device=embeddings.device)
        
        loss = F.cross_entropy(logits, targets)
        return loss

class HypersphereLoss(nn.Module):
    """Hypersphere loss to minimize volume of normal data representation"""
    
    def __init__(self, center_momentum: float = 0.9):
        super().__init__()
        self.center_momentum = center_momentum
        self.register_buffer('center', None)
        
    def forward(self, embeddings: torch.Tensor) -> torch.Tensor:
        """
        Hypersphere loss to learn compact normal representation
        """
        # Normalize embeddings
        embeddings = F.normalize(embeddings, p=2, dim=1)
        
        # Update center without tracking gradients across batches
        with torch.no_grad():
            batch_center = embeddings.mean(dim=0)
            if self.center is None:
                # Initialize the buffer on first use
                self.register_buffer('center', batch_center.detach().clone())
            else:
                # EMA update: center = m*center + (1-m)*batch_center
                self.center.mul_(self.center_momentum).add_(batch_center.detach() * (1.0 - self.center_momentum))
        
        # Compute distances to the (detached) center
        distances = torch.norm(embeddings - self.center, p=2, dim=1)
        
        # Return mean distance (we want to minimize this for normal samples)
        return distances.mean()

def create_waf_model(vocab_size: int = 10000) -> Tuple[WAFTransformer, WAFTokenizer]:
    """Create WAF model and tokenizer"""
    config = WAFTransformerConfig(vocab_size=vocab_size)
    model = WAFTransformer(config)
    tokenizer = WAFTokenizer(vocab_size=vocab_size)
    
    return model, tokenizer

if __name__ == "__main__":
    # Test the model
    model, tokenizer = create_waf_model()
    
    # Add some sample tokens
    sample_tokens = ['GET', 'POST', '/api/users', '/blog/posts', 'HTTP/1.1', '200', '404', 'Mozilla']
    for token in sample_tokens:
        tokenizer.add_token(token)
        
    # Test tokenization
    test_sequence = ['GET', '/api/users', '200', 'Mozilla']
    encoded = tokenizer.encode(test_sequence, max_length=64)
    
    print("Input shape:", encoded['input_ids'].shape)
    print("Input IDs:", encoded['input_ids'])
    
    # Test model forward pass
    model.eval()
    with torch.no_grad():
        outputs = model(
            input_ids=encoded['input_ids'].unsqueeze(0),
            attention_mask=encoded['attention_mask'].unsqueeze(0)
        )
        
    print("MLM logits shape:", outputs['mlm_logits'].shape)
    print("Anomaly score:", outputs['anomaly_score'].item())
    print("Model created successfully!")
