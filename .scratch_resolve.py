import re

def conflict(s):
    m = re.search(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]*\n', s, re.S)
    assert m, 'no conflict block'
    return m

# ---------- 1) BEHAVIORAL_DEVIATIONS.md ----------
p = 'docs/BEHAVIORAL_DEVIATIONS.md'
s = open(p, encoding='utf-8').read()
m = conflict(s)
taint = [l for l in m.group(1).split('\n') if l.startswith('| BD-')]
assert len(taint) == 1
taint_row = taint[0].replace('| BD-15 |', '| BD-17 |', 1)
s = s[:m.start()] + m.group(2).rstrip('\n') + '\n' + taint_row + '\n' + s[m.end():]
open(p, 'w', encoding='utf-8').write(s)
print('BD file: main BD-15 (FX motion) + BD-16 (beam seam) kept; taint row -> BD-17')

# ---------- 2) CHANGELOG.md : union ----------
p = 'CHANGELOG.md'
s = open(p, encoding='utf-8').read()
m = conflict(s)
main_lines = m.group(2).split('\n')
head = [i for i, l in enumerate(main_lines) if l.startswith('## [Unreleased]')]
assert head, main_lines[:3]
bul = [l for l in m.group(1).split('\n') if l.strip()]
k = [i for i, l in enumerate(bul) if l.startswith('**Taint')]
assert k, bul[0]
out = main_lines[:head[0] + 1] + [''] + bul[k[0]:] + [''] + main_lines[head[0] + 1:]
s = s[:m.start()] + '\n'.join(out) + '\n' + s[m.end():]
open(p, 'w', encoding='utf-8').write(s)
print('CHANGELOG: taint bullet placed under main Unreleased heading')

# ---------- 3) feature-tracking.md : the branch's taint row wins ----------
p = 'docs/feature-tracking.md'
s = open(p, encoding='utf-8').read()
m = conflict(s)
assert m.group(1).count('| Taint |') == 1
s = s[:m.start()] + m.group(1) + '\n' + s[m.end():]
open(p, 'w', encoding='utf-8').write(s)
print('feature-tracking: taint row = branch version (seed-stream finding)')

# ---------- 4) renumber taint-side BD references ----------
def renum_lines(lines, pred):
    out, n = [], 0
    for l in lines:
        if pred(l) and ('BD-15' in l or 'BD-16' in l):
            l = l.replace('BD-15', 'BD-17').replace('BD-16', 'BD-18')
            n += 1
        out.append(l)
    return out, n

java = [
    'src/main/java/com/jonnyutah12/thaumcraftclassic/taint/TaintBiomes.java',
    'src/main/java/com/jonnyutah12/thaumcraftclassic/taint/TaintManager.java',
    'src/main/java/com/jonnyutah12/thaumcraftclassic/taint/TaintRules.java',
    'src/main/java/com/jonnyutah12/thaumcraftclassic/taint/TaintConversion.java',
    'src/main/java/com/jonnyutah12/thaumcraftclassic/gametest/TaintCleansingGameTest.java',
]
docs = ['docs/feature-tracking.md', 'docs/FEATURE_MATRIX.md', 'docs/BEHAVIORAL_DEVIATIONS.md', 'CHANGELOG.md']
for p in java:
    try:
        lines = open(p, encoding='utf-8').read().split('\n')
    except FileNotFoundError:
        continue
    out, n = renum_lines(lines, lambda l: True)
    if n:
        open(p, 'w', encoding='utf-8').write('\n'.join(out))
    print(f'{p}: {n} taint-code BD refs renumbered')
for p in docs:
    lines = open(p, encoding='utf-8').read().split('\n')
    out, n = renum_lines(lines, lambda l: 'aint' in l)
    if n:
        open(p, 'w', encoding='utf-8').write('\n'.join(out))
    print(f'{p}: {n} taint-line BD refs renumbered')
