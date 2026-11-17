#!/usr/bin/env python3
"""Render a separate pilot export through Claude's existing tools/preview, without asset writes."""
import argparse
import copy
import json
from pathlib import Path
from import_weapon_animation import ROOT
import tl
from run import load

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('export',type=Path)
parser.add_argument('--name',default='bb_test_guard')
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
a,model,fp,tp,grip=load('pale_linear_greatsword')
a=copy.deepcopy(a)
pilot=json.loads(args.export.read_text())['animations']
if any(not n.startswith('bb_') for n in pilot):parser.error('Expected separate bb_ pilot clips')
a['animations'].update(pilot)
# Supply the in-memory merged library to the established renderer, never the live file.
original_load=tl.load
tl.load=lambda arch:(a,model,fp,tp,grip) if arch=='pale_linear_greatsword' else original_load(arch)
args.output.mkdir(parents=True,exist_ok=True)
L=pilot[args.name]['length']
for pair in (False,True):
    stem='dual' if pair else 'single'
    tl.build('pale_linear_greatsword',[(args.name,args.name+'_off' if pair else None,L)],pair,
             str(args.output/(stem+'.gif')),fps=20,front=True,widescreen=True,
             fp_out=str(args.output/(stem+'_fp.gif')))
print(args.output)
