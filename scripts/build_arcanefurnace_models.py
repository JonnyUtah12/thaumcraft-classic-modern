#!/usr/bin/env python3
"""The alchemical furnace's face machine, shipped as block states (BD-18, t_fd5265f3).

4.2.3.5 picked the furnace's faces inside its render hooks, from live tile data,
every frame. 1.21.1 has no such hook, so the tile publishes the original's
condition into block state properties (``stack`` = the vertical-run level,
``attached`` = the side the machine's other half touches) and this script writes
the models the shipped blockstate names per value (D-020 in
docs/porting-decisions.md). D-004: the models are port-authored from the
original's *geometry*, committed here; no model file or PNG is copied out of
reference/assets.

Where each number comes from (reference/decompiled/thaumcraft/thaumcraft/):

``common/blocks/BlockArcaneFurnace.java:43-49``
    registers 27 icons ``thaumcraft:furnace<N>`` for ``N < 27`` **skipping 8 and
    24**. Neither name is ever registered, so no model below may name it.

``common/blocks/BlockArcaneFurnace.java:57-156`` (``calculateTexture``)
    metadata 0 (the base plate): the per-side switches answer
    ``level != 9 ? icon[7] : icon[6]`` for the faces they name; the ISMR draws
    this block with an override, so the faces below are the lava the override
    supplies, and the side sprite is what the *item* model shows.
    metadata 10 (the nozzle): names none of the per-side faces, so every face
    falls through to ``level != 9 ? icon[7] : icon[6]`` — the sprite the
    ``stack`` property swaps (``furnace7`` / ``furnace6``).

``common/blocks/BlockArcaneFurnace.java:71-77``
    top/bottom for metadata 10 answer ``icon[27]`` on a 27-slot array: an
    out-of-range read, i.e. there is no legitimate sprite for those faces. The
    model names the side sprite the block already ships (``furnace7``), the
    face-table probe pins that choice, and BD-18 records the delta.

``common/blocks/BlockArcaneFurnace.java:158-177`` (``calculateLevel``)
    9 = mid of a vertical run of same-metadata furnaces, 18 = top of a run,
    0 = standalone. Shipped as ``stack`` 2 / 1 / 0.

``client/renderers/block/BlockArcaneFurnaceRenderer.java:16-59`` (72 lines —
the card's ":45-107" is out of range; the file ends at :72)
    metadata 0 (``:19-23``): the cube is drawn with
    ``renderer.overrideBlockTexture = Blocks.lava.getIcon(0)`` — every face of
    the base plate is lava — then the fire pass ``func_147784_q(block, x, y, z)``
    draws vanilla's fire cross on the same block, and the block is 1 block tall
    (``setRenderBounds(0,0,0,1,1,1)``), so the cross is the vanilla 22.4/16-tall
    geometry from minecraft's own ``models/block/template_fire_floor.json``.
    metadata 10 (``:26-56``, a 4-way branch west / east / north / else +Z):
    ``renderFaceXNeg(RenderBlocks.func_147734_d(BlockArcaneFurnace.class,
    "furnace13"), x - W10, y, z)`` with ``W10 = 0.625``
    (``client/renderers/block/BlockRenderer.java:22``) → 10/16 outside the cell,
    the ``furnace15`` face at 0.8 → 12.8/16, then
    ``setRenderBounds(0, 0, 0, 1, 1.5, 1)`` (``:33``) and a fire face
    (``Blocks.field_150480_ab`` = fire) at 0.9 → 14.4/16, i.e. a flame quad
    24/16 tall.

Every sprite named below is a published row of docs/asset-mapping.csv:
``thaumcraft:block/furnace6|7|13|15`` (ARR overlay, D-004) and the vanilla
``minecraft:block/lava_still`` / ``minecraft:block/fire_0`` (1.21.1 has no
``block/lava.png``; verified in the 1.21.1 client jar). ``ironwood``,
``metalplate`` and ``goldplate`` are NOT published and are never named.

Writes:
  src/main/resources/assets/thaumcraft/models/block/blockarcanefurnace_v0_lit.json
  src/main/resources/assets/thaumcraft/models/block/blockarcanefurnace_v1_<side>[_stacked].json
  src/main/resources/assets/thaumcraft/blockstates/blockarcanefurnace.json
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/thaumcraft"
MODELS = ASSETS / "models/block"
BLOCKSTATES = ASSETS / "blockstates"

SPRITE_LAVA = "minecraft:block/lava_still"
SPRITE_FIRE = "minecraft:block/fire_0"
FURNACE6 = "thaumcraft:block/furnace6"
FURNACE7 = "thaumcraft:block/furnace7"
FURNACE13 = "thaumcraft:block/furnace13"
FURNACE15 = "thaumcraft:block/furnace15"

# calculateLevel's answer, as the shipped property's value.
STACK_TOP = 1
STACK_MID = 2

# Direction 3D data values (DOWN 0, UP 1, NORTH 2, SOUTH 3, WEST 4, EAST 5).
ATTACHED_NONE = 6
ART_SIDE_FOR_ATTACHED = {4: "west", 5: "east", 2: "north", 3: "south"}
# The ISMR's branch is west / east / north / else-+Z: a partner above or below
# (values 1 and 0) draws the +Z (south) art, and so does no partner at all.
FALLBACK_ART_SIDE = "south"

AXIS = {"west": 0, "east": 0, "up": 1, "down": 1, "north": 2, "south": 2}
POSITIVE = {"east", "up", "south"}

# The original's three offsets out of the nozzle's own cell, in 1/16 blocks.
PLATE_DEPTH = 10.0          # x - W10, W10 = 0.625
NOZZLE_DEPTH = 12.8         # x - 0.8
FIRE_DEPTH = 14.4           # x - 0.9
QUAD_THICKNESS = 0.16       # the glow-shell precedent's hairline thickness


def art_element(side: str, depth: float, texture: str, height: float = 16.0) -> dict:
    """One quad the ISMR drew outside the nozzle's cell, on ``side``.

    The quad is centred on ``depth``/16 blocks outside the nozzle's own face,
    hairline-thick (the ``blocklifter_lifter_glow`` shell's precedent), spans the
    cell's two perpendicular axes and ``height``/16 in y."""
    assert side in ("west", "east", "north", "south"), side
    axis = AXIS[side]
    half = QUAD_THICKNESS / 2.0
    lo, hi = (16.0 + depth - half, 16.0 + depth + half) if side in POSITIVE \
        else (-depth - half, -depth + half)
    box_from = [0.0, 0.0, 0.0]
    box_to = [16.0, 16.0, 16.0]
    box_from[axis] = lo
    box_to[axis] = hi
    box_to[1] = height
    return {
        "from": [round(v, 3) for v in box_from],
        "to": [round(v, 3) for v in box_to],
        "faces": {side: {"uv": [0, 0, 16, 16], "texture": texture}},
    }


def body_element(sides: str, caps: str) -> dict:
    """The nozzle's own cube: 1.7.10's full-block bounds (metadata 10). The two
    arguments are the sprites the model's ``side``/``cap`` aliases carry."""
    faces = {
        face: {
            "uv": [0, 0, 16, 16],
            "texture": "#side" if face in ("north", "south", "west", "east") else "#cap",
            "cullface": face,
        }
        for face in ("down", "up", "north", "south", "west", "east")
    }
    assert sides and caps
    return {"from": [0.0, 0.0, 0.0], "to": [16.0, 16.0, 16.0], "faces": faces}


def plate_model() -> dict:
    """The base plate: the lava the ISMR overrides every face to, plus the fire
    cross of the pass that runs right after the cube. The cross's geometry is
    vanilla's own ``models/block/template_fire_floor.json`` (four quads, rotated
    +-22.5 deg, y 0 -> 22.4)."""
    cube = {
        "from": [0.0, 0.0, 0.0],
        "to": [16.0, 16.0, 16.0],
        "faces": {
            face: {"uv": [0, 0, 16, 16], "texture": "#lava", "cullface": face}
            for face in ("down", "up", "north", "south", "west", "east")
        },
    }
    cross = [
        {
            "from": [-0.5, 0.0, 8.0],
            "to": [16.5, 22.4, 8.0],
            "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "z", "angle": 22.5},
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#fire"},
                "south": {"uv": [0, 16, 16, 0], "texture": "#fire"},
            },
        },
        {
            "from": [-0.5, 0.0, 8.0],
            "to": [16.5, 22.4, 8.0],
            "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "z", "angle": -22.5},
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#fire"},
                "south": {"uv": [0, 16, 16, 0], "texture": "#fire"},
            },
        },
        {
            "from": [8.0, 0.0, -0.5],
            "to": [8.0, 22.4, 16.5],
            "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "x", "angle": 22.5},
            "faces": {
                "east": {"uv": [0, 0, 16, 16], "texture": "#fire"},
                "west": {"uv": [0, 16, 16, 0], "texture": "#fire"},
            },
        },
        {
            "from": [8.0, 0.0, -0.5],
            "to": [8.0, 22.4, 16.5],
            "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "x", "angle": -22.5},
            "faces": {
                "east": {"uv": [0, 0, 16, 16], "texture": "#fire"},
                "west": {"uv": [0, 16, 16, 0], "texture": "#fire"},
            },
        },
    ]
    return {
        "textures": {"lava": SPRITE_LAVA, "fire": SPRITE_FIRE},
        "elements": [cube] + cross,
    }


def nozzle_model(side: str, stack: int) -> dict:
    """The nozzle: its own cube plus the three art quads the ISMR drew on the
    side of the base plate it found. ``stack`` is the published run level, which
    is what the original's ``calculateTexture`` used for these faces
    (``level != 9 ? icon[7] : icon[6]``)."""
    sides = FURNACE6 if stack == STACK_MID else FURNACE7
    caps = FURNACE7  # the original's answer for metadata 10 is icon[27]: no sprite
    return {
        "textures": {
            "side": sides,
            "cap": caps,
            "plate": FURNACE13,
            "nozzle": FURNACE15,
            "fire": SPRITE_FIRE,
        },
        "elements": [
            body_element(sides, caps),
            art_element(side, PLATE_DEPTH, "#plate"),
            art_element(side, NOZZLE_DEPTH, "#nozzle"),
            # The flame is drawn after the block is re-bounded to 1.5 tall.
            art_element(side, FIRE_DEPTH, "#fire", height=24.0),
        ],
    }


# --------------------------------------------------------------------- shell
# TC-M4-06-m (t_5eb27d02): the nine shell metas 1-9 of the 3x3x3 machine, and
# the face rule ``calculateTexture`` answers for them
# (``common/blocks/BlockArcaneFurnace.java:57-156``), transcribed branch by
# branch:
#
# ``:61-63``  ``add = 3`` when the block touching THIS face is this block with
#             metadata 10 — the nozzle (``BlockUtils.java:342-379``) — and
#             ``add`` then joins the face index on the four side faces.
# ``:66-70``  side 1 (up) at the top of a vertical run (``level == 18``):
#             metas 2, 4, 6, 8 answer a cap of their own — 16, 17, 26, 25.
# ``:71-84``  the caps (side 0 down, side 1 up) with no nozzle on that face:
#             metadata 5 answers ``icon[10]``; every other metadata answers
#             ``icon[(meta-1)%3 + (meta-1)/3*9]`` — the 3x3 grid position inside
#             the 27-icon atlas, which is exactly the scan order
#             ``WandManager.replaceArcaneFurnace`` (:457-480) numbered the
#             machine's cells with. ``add == 3`` falls through to ``:85``'s
#             ``default:``, which answers ``icon[7]`` / ``icon[6]``.
# ``:86-127``  the four side faces: 1.7.10's ordinals are 0 down, 1 up, 2 north,
#             3 south, 4 west, 5 east (the ISMR's west/east/north/else-+Z branch
#             at ``BlockArcaneFurnaceRenderer.java:26-56`` pins the mapping).
#             Each side names the three columns that face it — north 1/2/3,
#             south 7/8/9, west 1/4/7, east 3/6/9 — as
#             ``icon[k + level + add]``, and every other metadata (the corners)
#             falls to ``level != 9 ? icon[7] : icon[6]``.
# ``:158-177``  ``calculateLevel``: 9 = mid of a vertical run, 18 = top of a
#             run, 0 = standalone. The port ships that as ``stack`` 2 / 1 / 0.
#
# The icons this rule can answer are 0-7, 9-14, 16-23, 25, 26 — icons 8 and 24
# are never registered (``:43-49``) and the rule never reaches them, which is
# what the assertions below check.
SHELL_SIDES = ("down", "up", "north", "south", "west", "east")
SHELL_LEVEL_FOR_STACK = {0: 0, 1: 18, 2: 9}
SHELL_GRID_OF_META = {meta: (meta - 1) % 3 + (meta - 1) // 3 * 9 for meta in range(1, 10)}
SHELL_TOP_CAP_AT_18 = {2: 16, 4: 17, 6: 26, 8: 25}
SHELL_SIDE_COLUMN = {
    2: {1: 2, 2: 1, 3: 0},   # north
    3: {7: 0, 8: 1, 9: 2},   # south
    4: {1: 0, 4: 1, 7: 2},   # west
    5: {3: 2, 6: 1, 9: 0},   # east
}


def shell_icon(meta: int, stack: int, add: int, side: int) -> int:
    """``calculateTexture``'s icon index for shell metadata ``meta`` (1-9) on
    1.7.10's side ``side``, at the run level ``stack`` answers, with the
    neighbour gate ``add`` (0 or 3)."""
    level = SHELL_LEVEL_FOR_STACK[stack]
    if side in (0, 1):
        if side == 1 and level == 18 and meta in SHELL_TOP_CAP_AT_18:
            return SHELL_TOP_CAP_AT_18[meta]
        if add != 3:
            return 10 if meta == 5 else SHELL_GRID_OF_META[meta]
        return 6
    column = SHELL_SIDE_COLUMN[side]
    if meta in column:
        return column[meta] + level + add
    return 7 if level != 9 else 6


def shell_model(meta: int, stack: int, attached: int) -> dict:
    """One shell cell's model: the full cube 1.7.10's metadata 1-9 drew (its
    bounds are the whole block, ``setBlockBoundsBasedOnState`` :193-213 has no
    branch for the shell metas), with every face's sprite answered by
    ``calculateTexture`` (:57-156) for this metadata, this run level and this
    ``attached`` side — the side whose face gets the ``add = 3`` answer
    (``:61-63``, ``BlockUtils.java:342-379``)."""
    faces = {}
    for index, face in enumerate(SHELL_SIDES):
        add = 3 if index == attached else 0
        icon = shell_icon(meta, stack, add, index)
        faces[face] = {
            "uv": [0, 0, 16, 16],
            "texture": f"thaumcraft:block/furnace{icon}",
            "cullface": face,
        }
    return {"elements": [{"from": [0.0, 0.0, 0.0], "to": [16.0, 16.0, 16.0], "faces": faces}]}


def write(path: Path, document: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    written: list[Path] = []

    plate = MODELS / "blockarcanefurnace_v0_lit.json"
    write(plate, plate_model())
    written.append(plate)

    variants: dict[str, dict] = {}
    for stack in (0, 1, 2):
        for attached in range(0, 7):
            side = ART_SIDE_FOR_ATTACHED.get(attached, FALLBACK_ART_SIDE)
            name = f"blockarcanefurnace_v1_{side}" + ("_stacked" if stack == STACK_MID else "")
            model = MODELS / f"{name}.json"
            write(model, nozzle_model(side, stack))
            written.append(model)
            # The base plate ignores the face machine's inputs — the ISMR drew it
            # the same way whatever its neighbours were — but the key names every
            # property so no wildcard can pin one model for two values.
            variants[f"variant=0,stack={stack},attached={attached}"] = {
                "model": "thaumcraft:block/blockarcanefurnace_v0_lit"}
            variants[f"variant=1,stack={stack},attached={attached}"] = {
                "model": f"thaumcraft:block/{name}"}

    # TC-M4-06-m: the nine shell metas of the 3x3x3, one model per
    # (variant, stack, attached) — the face machine's three inputs are the same
    # three properties the plate and the nozzle publish, so the shell needs no
    # new property. The shell's variant is the original's metadata + 1, so
    # variant 2..10 carries grid 1..9.
    for stack in (0, 1, 2):
        for attached in range(0, 7):
            for grid in range(1, 10):
                variant = grid + 1
                name = f"blockarcanefurnace_v{variant}"
                if stack:
                    name += f"_stack{stack}"
                if attached != 6:
                    name += f"_a{attached}"
                model = MODELS / f"{name}.json"
                write(model, shell_model(grid, stack, attached))
                written.append(model)
                variants[f"variant={variant},stack={stack},attached={attached}"] = {
                    "model": f"thaumcraft:block/{name}"}

    blockstate = BLOCKSTATES / "blockarcanefurnace.json"
    write(blockstate, {"variants": variants})
    written.append(blockstate)

    for path in written:
        print(f"wrote {path.relative_to(ROOT)}")
    print(f"{len(written) - 1} models, {len(variants)} blockstate keys")


if __name__ == "__main__":
    main()
