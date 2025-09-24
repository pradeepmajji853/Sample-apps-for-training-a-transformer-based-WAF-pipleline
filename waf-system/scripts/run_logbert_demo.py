#!/usr/bin/env python3
import os
import sys
import json
import glob
from pathlib import Path
from datetime import datetime, timedelta
import random
from urllib.parse import quote_plus
import numpy as np
import pandas as pd

# Resolve WAF root
WAF_ROOT = Path(__file__).resolve().parents[1]

# Ensure local ml-pipeline packages are importable
ml_pkg = WAF_ROOT / 'ml-pipeline'
sys.path.insert(0, str(ml_pkg))
sys.path.insert(0, str(ml_pkg / 'training'))
sys.path.insert(0, str(ml_pkg / 'preprocessing'))

# Robust imports
try:
    from ml_pipeline.preprocessing.log_processor import LogPreprocessor
    from ml_pipeline.training.trainer import WAFTrainer, prepare_training_data, collate_fn
    from ml_pipeline.training.waf_model import create_waf_model
except Exception:
    from log_processor import LogPreprocessor  # type: ignore
    from trainer import WAFTrainer, prepare_training_data, collate_fn  # type: ignore
    from waf_model import create_waf_model  # type: ignore

from torch.utils.data import DataLoader
import torch
from sklearn.metrics import precision_recall_fscore_support, accuracy_score, roc_auc_score

# Synthetic benign log generation
USE_SYNTHETIC_BENIGN = True
SYNTH_COUNT = 10_000
BENIGN_SYNTH_PATH = (WAF_ROOT / 'data' / 'logs' / 'benign_synth.log')

USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15',
    'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:122.0) Gecko/20100101 Firefox/122.0',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148',
    'curl/8.4.0',
    'python-requests/2.31.0'
]
METHODS = (['GET'] * 85 + ['POST'] * 10 + ['PUT'] * 3 + ['DELETE'] * 2)
STATUS = ([200] * 85 + [304] * 5 + [204] * 5 + [301] * 3 + [302] * 2)
CATEGORIES = ['electronics', 'fashion', 'home', 'toys', 'books', 'sports', 'beauty']
TAGS = ['release', 'update', 'how-to', 'tips', 'news', 'guide', 'faq']
SEARCH_TERMS = [
    'summer dress', 'wireless earbuds', 'gaming mouse', 'sofa cover', 'coffee beans',
    'running shoes', 'yoga mat', 'cookbook', 'water bottle', 'phone case',
    'winter jacket', 'smart watch', 'hair dryer', 'lego set', 'tennis racket'
]
STATIC_FILES = [
    'app.css', 'theme.css', 'bundle.js', 'vendor.js', 'logo.png', 'banner.jpg',
    'app.min.css', 'app.min.js', 'favicon.ico', 'robots.txt', 'sitemap.xml'
]
REFERRERS = ['-', 'https://www.google.com/', 'https://www.bing.com/', 'https://example.com/ecommerce/', 'https://example.com/blog-cms/']
IP_OCTET = list(range(1, 255))

def rand_ip():
    return f"{random.choice(IP_OCTET)}.{random.randint(0,255)}.{random.randint(0,255)}.{random.choice(IP_OCTET)}"

def ts_apache(delta_minutes: int | None = None) -> str:
    if delta_minutes is None:
        delta_minutes = random.randint(0, 60 * 24)
    t = datetime.utcnow() - timedelta(minutes=delta_minutes, seconds=random.randint(0, 59))
    return t.strftime('%d/%b/%Y:%H:%M:%S +0000')

def ecommerce_path():
    r = random.random()
    if r < 0.10:
        return '/ecommerce/'
    elif r < 0.25:
        return '/ecommerce/products'
    elif r < 0.50:
        return f"/ecommerce/product/{random.randint(1, 5000)}"
    elif r < 0.70:
        term = quote_plus(random.choice(SEARCH_TERMS))
        return f"/ecommerce/search?q={term}&page={random.randint(1, 5)}&sort=price_asc"
    elif r < 0.85:
        cat = random.choice(CATEGORIES)
        return f"/ecommerce/category/{cat}?page={random.randint(1, 10)}"
    else:
        return f"/ecommerce/static/{random.choice(STATIC_FILES)}"

def blog_path():
    r = random.random()
    if r < 0.20:
        return '/blog-cms/'
    elif r < 0.55:
        slug = f"post-{random.randint(1, 3000)}"
        return f"/blog-cms/post/{slug}"
    elif r < 0.80:
        tag = random.choice(TAGS)
        return f"/blog-cms/tag/{tag}?page={random.randint(1, 8)}"
    else:
        return f"/blog-cms/static/{random.choice(STATIC_FILES)}"

def api_path():
    r = random.random()
    if r < 0.35:
        return '/rest-api/api/users'
    elif r < 0.55:
        return f"/rest-api/api/users/{random.randint(1, 5000)}"
    elif r < 0.75:
        return '/rest-api/api/orders'
    elif r < 0.90:
        return f"/rest-api/api/orders/{random.randint(1000, 9999)}"
    else:
        return '/rest-api/api/health'

def static_path():
    base = random.choice(['/assets', '/static', '/public'])
    return f"{base}/{random.choice(STATIC_FILES)}"

def benign_path():
    return random.choices([ecommerce_path, blog_path, api_path, static_path], weights=[45, 25, 20, 10], k=1)[0]()

def benign_line():
    ip = rand_ip()
    ident = '-'
    authuser = '-'
    ts = ts_apache()
    method = random.choice(METHODS)
    uri = benign_path()
    proto = 'HTTP/1.1'
    status = random.choice(STATUS)
    size = random.randint(120, 524288)
    ref = random.choice(REFERRERS)
    ua = random.choice(USER_AGENTS)
    return f'{ip} {ident} {authuser} [{ts}] "{method} {uri} {proto}" {status} {size} "{ref}" "{ua}"'

def ensure_benign_dataset():
    BENIGN_SYNTH_PATH.parent.mkdir(parents=True, exist_ok=True)
    if USE_SYNTHETIC_BENIGN and not BENIGN_SYNTH_PATH.exists():
        with BENIGN_SYNTH_PATH.open('w', encoding='utf-8') as f:
            for _ in range(SYNTH_COUNT):
                f.write(benign_line() + '\n')
        print(f'Generated {SYNTH_COUNT} benign log lines at: {BENIGN_SYNTH_PATH}')
    else:
        print(f'Using benign log file: {BENIGN_SYNTH_PATH} (exists={BENIGN_SYNTH_PATH.exists()})')

# Build sequences from logs

def build_sequences(max_lines: int = 10_000):
    preprocessor = LogPreprocessor()
    log_paths = [str(BENIGN_SYNTH_PATH)] if USE_SYNTHETIC_BENIGN else [
        str(WAF_ROOT / 'data' / 'logs' / 'access.log'),
        str(WAF_ROOT / 'tomcat' / 'current' / 'logs' / 'localhost_access_log*.txt'),
    ]
    sequences = []
    for pattern in log_paths:
        expanded = glob.glob(pattern) if any(ch in pattern for ch in '*?[]') else [pattern]
        for path_str in expanded:
            p = Path(path_str)
            if not p.exists() or not p.is_file():
                continue
            with p.open('r', errors='ignore') as f:
                for line in f:
                    if len(sequences) >= max_lines:
                        break
                    pl = line.strip()
                    if not pl:
                        continue
                    processed = preprocessor.process_log_entry(pl)
                    if not processed:
                        continue
                    parsed = processed['parsed']
                    seq = [
                        parsed.get('method', 'GET'),
                        parsed.get('path_only', '/'),
                        str(parsed.get('status', 200)),
                    ]
                    ua = parsed.get('http_user_agent', '') or ''
                    if 'Mozilla' in ua:
                        seq.append('Mozilla')
                    elif 'curl' in ua:
                        seq.append('curl')
                    elif 'python' in ua.lower():
                        seq.append('python')
                    else:
                        seq.append('Other-Agent')
                    sequences.append(seq)
    if len(sequences) < 50:
        base = [
            ['GET', '/ecommerce/', '200', 'Mozilla'],
            ['GET', '/ecommerce/products', '200', 'Mozilla'],
            ['GET', '/blog-cms/', '200', 'Mozilla'],
            ['GET', '/rest-api/api/users', '200', 'curl']
        ]
        sequences.extend(base * 50)
    return sequences

@torch.no_grad()
def score_request(trainer, tokenizer, preprocessor, method: str, uri: str, user_agent: str = 'Mozilla/5.0', threshold: float = 0.5):
    ts = datetime.utcnow().strftime('%d/%b/%Y:%H:%M:%S +0000')
    line = f'127.0.0.1 - - [{ts}] "{method} {uri} HTTP/1.1" 200 0 "-" "{user_agent}"'
    processed = preprocessor.process_log_entry(line)
    if not processed:
        return 0.0, False
    parsed = processed['parsed']
    seq = [parsed.get('method', 'GET'), parsed.get('path_only', '/'), str(parsed.get('status', 200))]
    ua = parsed.get('http_user_agent', '') or ''
    if 'Mozilla' in ua:
        seq.append('Mozilla')
    elif 'curl' in ua:
        seq.append('curl')
    elif 'python' in ua.lower():
        seq.append('python')
    else:
        seq.append('Other-Agent')
    enc = tokenizer.encode(seq, max_length=128)
    input_ids = enc['input_ids'].unsqueeze(0)
    attention_mask = enc['attention_mask'].unsqueeze(0)
    outputs = trainer.model(input_ids=input_ids, attention_mask=attention_mask)
    score = float(outputs['anomaly_score'].item())
    return score, score > threshold

# Helper to convert numpy/pandas types to native Python for JSON serialization
def to_native(obj):
    import numpy as _np
    import pandas as _pd
    if isinstance(obj, dict):
        return {k: to_native(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [to_native(v) for v in obj]
    if isinstance(obj, (_np.floating, )):
        return float(obj)
    if isinstance(obj, (_np.integer, )):
        return int(obj)
    if isinstance(obj, _np.ndarray):
        return obj.tolist()
    if isinstance(obj, _pd.Series):
        return to_native(obj.to_dict())
    return obj

def main():
    ensure_benign_dataset()
    sequences = build_sequences(max_lines=SYNTH_COUNT)
    model, tokenizer = create_waf_model()
    train_ds, val_ds = prepare_training_data(sequences, tokenizer)
    train_loader = DataLoader(train_ds, batch_size=32, shuffle=True, collate_fn=collate_fn)
    val_loader = DataLoader(val_ds, batch_size=32, shuffle=False, collate_fn=collate_fn)
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    trainer = WAFTrainer(model, tokenizer, device=device)
    train_metrics = trainer.train_epoch(train_loader)
    val_metrics = trainer.evaluate(val_loader)
    # Save model
    models_dir = WAF_ROOT / 'data' / 'models'
    models_dir.mkdir(parents=True, exist_ok=True)
    save_path = models_dir / 'notebook_model.pt'
    trainer.save_model(str(save_path))

    # Evaluation vs malicious payloads
    preprocessor = LogPreprocessor()
    benign_uris = ['/ecommerce/', '/ecommerce/products', '/blog-cms/', '/rest-api/api/users'] * 10
    malicious_payloads = [
        "/ecommerce/search?q=' OR '1'='1",
        "/ecommerce/search?q=%27%20OR%201%3D1--",
        "/ecommerce/product?id=1;DROP TABLE users;--",
        "/blog-cms/?q=<script>alert(1)</script>",
        "/rest-api/api/users/../../../../etc/passwd",
        "/rest-api/api/users?name=`cat /etc/passwd`",
    ]
    rows = []
    for uri in random.sample(benign_uris, min(50, len(benign_uris))):
        s, yhat = score_request(trainer, tokenizer, preprocessor, 'GET', uri)
        rows.append({'uri': uri, 'label': 0, 'score': s, 'pred': int(yhat)})
    for uri in malicious_payloads:
        s, yhat = score_request(trainer, tokenizer, preprocessor, 'GET', uri)
        rows.append({'uri': uri, 'label': 1, 'score': s, 'pred': int(yhat)})
    df = pd.DataFrame(rows)
    y_true = df['label'].values
    y_pred = df['pred'].values
    y_score = df['score'].values
    acc = accuracy_score(y_true, y_pred)
    prec, rec, f1, _ = precision_recall_fscore_support(y_true, y_pred, average='binary')
    try:
        auc = roc_auc_score(y_true, y_score)
    except Exception:
        auc = float('nan')

    # Ensure DataFrame numeric columns are explicitly typed
    df['label'] = df['label'].astype(int)
    df['pred'] = df['pred'].astype(int)
    df['score'] = df['score'].astype(float)

    metrics = {'accuracy': float(acc), 'precision': float(prec), 'recall': float(rec), 'f1': float(f1), 'roc_auc': float(auc) if auc == auc else None,
               'train': to_native(train_metrics), 'val': to_native(val_metrics)}
    out_dir = WAF_ROOT / 'data' / 'training'
    out_dir.mkdir(parents=True, exist_ok=True)
    top_records = df.sort_values('score', ascending=False).head(10).to_dict(orient='records')
    payload = {'metrics': to_native(metrics), 'top_suspicious': to_native(top_records)}
    with (out_dir / 'notebook_metrics.json').open('w') as f:
        json.dump(payload, f, indent=2)
    print(json.dumps(metrics, indent=2))
    print('Saved metrics to', out_dir / 'notebook_metrics.json')

if __name__ == '__main__':
    main()
