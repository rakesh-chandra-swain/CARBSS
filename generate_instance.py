#!/usr/bin/env python3
"""
Instance generator for CARBSS scheduling problem.
Provided by ScoreMe Solutions - DO NOT MODIFY.

Usage:
    python generate_instance.py --n 50 --K 8 --density 0.3 --seed 10 --output instance.json
"""

import random
import json
import argparse

def generate_instance(n, K, d=4, conflict_density=0.3, seed=42):
    """Generate a random MSME Credit Pipeline Scheduling instance."""
    random.seed(seed)
    
    tasks = [f'T{i}' for i in range(n)]
    
    conflicts = [(i, j) for i in range(n) for j in range(i+1, n)
                 if random.random() < conflict_density]
    
    cap = [32, 128, 8, 6.0]  # CPU, RAM, GPU, Network
    
    resources = [[random.uniform(1, cap[dim] // (n // K + 1))
                  for dim in range(4)] for _ in range(n)]
    
    capacities = [cap[:] for _ in range(K)]
    
    windows = [(lo := random.randint(0, K-2),
                random.randint(lo+1, K-1)) for _ in range(n)]
    
    weights = [random.uniform(1, 10) for _ in range(n)]
    
    return dict(
        tasks=tasks,
        conflicts=conflicts,
        resources=resources,
        capacities=capacities,
        windows=windows,
        weights=weights,
        K=K
    )

def main():
    parser = argparse.ArgumentParser(description='Generate CARBSS instance')
    parser.add_argument('--n', type=int, required=True, help='Number of tasks')
    parser.add_argument('--K', type=int, required=True, help='Number of slots')
    parser.add_argument('--density', type=float, default=0.3, help='Conflict density')
    parser.add_argument('--seed', type=int, default=42, help='Random seed')
    parser.add_argument('--output', type=str, required=True, help='Output JSON file')
    
    args = parser.parse_args()
    
    instance = generate_instance(args.n, args.K, conflict_density=args.density, seed=args.seed)
    
    with open(args.output, 'w') as f:
        json.dump(instance, f, indent=2)
    
    print(f"Generated instance: n={args.n}, K={args.K}, density={args.density}, seed={args.seed}")
    print(f"Conflicts: {len(instance['conflicts'])}")
    print(f"Output: {args.output}")

if __name__ == '__main__':
    main()
