#!/usr/bin/env python3
"""TC-M4-06-s doc rows: content-mapping.csv flips + the three tracking docs.

Run from the worktree root. Kept as a script so the doc flips are reproducible
and reviewable; the csv is edited byte-wise so its CRLF endings survive.
"""
import pathlib

W = pathlib.Path('.')
CRLF_ROWS = [
    'Thaumcraft:blockJar,',
    'Thaumcraft:blockArcaneFurnace,',
    'Thaumcraft:ItemEssence,',
    'Thaumcraft:BlockJarFilledItem,',
]

FEATURE_MATRIX_ROW = (
    '| Essentia storage (smelter, jars) (TC-M4-06-s) | arcane furnace base + nozzle '
    '(cook-time/extraction rates, smelting-bonus table), warded + void jars, jar labels, '
    'glass phial, filled-jar item | VERIFIED | t_d9fa0476 (this commit; replaces '
    't_e9d23e1c; salvage parent t_8aea81b5) | 8 game probes (jar holds one aspect to its '
    '64 capacity and hands a foreign aspect straight back, the void jar clips its overflow '
    'and keeps advertising suction 32/48 while a full plain jar drops to 0, the label '
    'filters the aspect exactly and lifts suction to 64, the furnace cook time measured off '
    'a real placed furnace and a real placed bellows = 140 cold / 80 fed FIRE / -20 per '
    'bellows capped at 3, raw iron consumed and its iron ingot ejected out of the nozzle '
    'side after a 170-tick run, the phial moving 8 units per fill and draining back, the '
    'filled jar dropping its essentia + label + void flag, and the four registry ids + the '
    '18 bonus rows resolving with the contract rows done/tested); 42 unit '
    '(JarStorageLogicTest 19 + ArcaneFurnaceLogicTest 16 + SmeltingBonusesTest 7) + 8 game '
    'green | D-001 id case; the original blockJar metadata 0..3 kept verbatim as the '
    'variant property so the landed recipe.138 (variant 3) stays valid | tube network '
    'TC-M4-07; the FIRE vis drain on TC-M5 (TileArcaneFurnace.drainVisFire returns 0 '
    'today); the 3x3x3 shell + bound blaze TC-M4-06-m; the brain (TC-M6-05) and node '
    '(TC-M6-03) jar tiles; furnace animation/sounds TC-M7-01, gui TC-M7-04 |'
)

COMPAT_OLD = ('| Essentia | Extraction, transport, matching, containers | NI (essentia '
              '*tables* are IT under Aspects; smelter/jars/tubes TC-M4-06/07) |')
COMPAT_NEW = (
    '| Essentia | Extraction, transport, matching, containers | PARTIAL (TC-M4-06-s / '
    't_d9fa0476: the infernal furnace base + nozzle with the original cook-time rates '
    '(140 cold, 80 fed FIRE, -20 per bellows capped at 3) and the 18 original '
    'smelting-bonus rows with the original roll (1-in-4 bare, 0.44 per bellows), warded + '
    'void jars with the original matching rules (one aspect, 64 cap, void clips, label '
    'filter, suction 32/64 vs 32/48, comparator floor(r*14)+1), the glass phial at 8 units '
    'per pass and the filled-jar drop carrying essentia + label + void; essentia *tables* '
    'remain IT under Aspects, the tube network is TC-M4-07, the FIRE vis drain TC-M5, the '
    '3x3x3 shell + blaze TC-M4-06-m |')

TRACKING_PREFIX = '| Essentia storage | TileSmelter*, jars |'
TRACKING_ROW = (
    '| Essentia storage (TC-M4-06-s) | '
    'tiles/{TileJarFillable,TileJarFillableVoid,JarStorageLogic,TileArcaneFurnace,'
    'TileArcaneFurnaceNozzle,ArcaneFurnaceLogic}, blocks/{BlockJar,BlockArcaneFurnace}, '
    'items/{ItemPhial,ItemJarFilled}, recipes/SmeltingBonuses, '
    'registration/catalog/CatalogEssentiaStorage | TC-M4-06 (t_e9d23e1c) -> TC-M4-06-s '
    '(t_d9fa0476) | **DONE (this commit)**: the essentia supply path is merged. '
    '`JarStorageLogic` + `ArcaneFurnaceLogic` carry the original rules as Level-free pure '
    'functions (the pattern the crucible and infusion cards use) so they unit-test; the '
    'tiles apply them. Jars: one aspect per jar, 64 capacity, the void jar clips its '
    'overflow instead of refusing, the label is an exact-match filter, suction 32/64 '
    '(plain) vs 32/48 (void, never 0), comparator read floor(r*14)+1, jar drops carry '
    'essentia + amount + label + the void flag. The blockJar metadata 0..3 are kept '
    'verbatim as the `variant` property (D-001) so the landed `recipe.138.arcane_shaped` '
    '(variant 3 = the void jar) keeps resolving; variants 1/2 (brain TC-M6-05, node '
    'TC-M6-03) create no tile. The furnace: cook time `(speedyTime > 0 ? 80 : 140) - 20 * '
    'bellows` capped at 3, the nozzle drawing 1 FIRE every 5 ticks under speedyTime 60 and '
    'advertising 128 suction under 40 (a fed furnace releases the line), inputs with no '
    'smelt result destroyed on the spot, results *ejected* out of the nozzle side with the '
    '`SmeltingBonuses` roll and the fractional experience. `SmeltingBonuses` is the 18 '
    'original `addSmeltingBonus` rows verbatim (count 0 semantics, the 32767 any-meta '
    'wildcard, ore-dictionary names as `c:` item tags) with lookup order exact item+meta '
    '-> wildcard -> tag. Evidence: 42 unit tests (JarStorageLogicTest 19, '
    'ArcaneFurnaceLogicTest 16, SmeltingBonusesTest 7) + `EssentiaStorageGameTest` 8 '
    'probes, including an end-to-end raw-iron -> iron-ingot eject run. The 4 registry rows '
    '+ the 18 `smelting_bonus` rows flipped to done/tested. Out of scope, with cards: tube '
    'network TC-M4-07, the FIRE vis drain TC-M5 (`drainVisFire` returns 0 today, the '
    'TileArcaneBore seam), the 3x3x3 shell + bound blaze TC-M4-06-m, the brain/node jar '
    'tiles, furnace animation/sounds TC-M7-01, gui TC-M7-04. The hungry chest was already '
    'merged (a9303ea, TC-M6-04d) - not duplicated |'
)

FM_PREFIX = '| Essentia storage (smelter, jars) |'


def replace_line(path, prefix, new_row):
    text = path.read_text()
    i = text.index(prefix)
    j = text.index('\n', i)
    path.write_text(text[:i] + new_row + text[j:])


def main():
    p = W / 'docs/content-mapping.csv'
    data = p.read_bytes()
    crlf = data.count(b'\r\n')
    lines = data.split(b'\r\n')
    flipped = []
    out = []
    for raw in lines:
        s = raw.decode('utf-8')
        owned = any(s.startswith(r) for r in CRLF_ROWS) or '.smelting_bonus,' in s
        if owned and ',not_started,not_tested,' in s:
            s = s.replace(',not_started,not_tested,', ',done,tested,')
            flipped.append(s.split(',')[1])
        out.append(s.encode('utf-8'))
    new = b'\r\n'.join(out)
    assert new.count(b'\r\n') == crlf, (new.count(b'\r\n'), crlf)
    p.write_bytes(new)
    print('csv rows flipped:', len(flipped))

    replace_line(W / 'docs/FEATURE_MATRIX.md', FM_PREFIX, FEATURE_MATRIX_ROW)
    compat = W / 'docs/compatibility-matrix.md'
    text = compat.read_text()
    assert COMPAT_OLD in text
    compat.write_text(text.replace(COMPAT_OLD, COMPAT_NEW))
    replace_line(W / 'docs/feature-tracking.md', TRACKING_PREFIX, TRACKING_ROW)
    print('docs updated')


if __name__ == '__main__':
    main()
