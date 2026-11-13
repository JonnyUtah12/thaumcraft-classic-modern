package com.jonnyutah12.thaumcraftclassic.wands;

import com.jonnyutah12.thaumcraftclassic.api.wands.WandCap;
import com.jonnyutah12.thaumcraftclassic.api.wands.WandRod;
import java.util.ArrayList;
import java.util.List;

/**
 * One entry of the {@code thaumcraft:wand_component} registry (TC-M6-01): every
 * rod/cap the original registered under one component tag.
 *
 * <p>The original keyed {@code WandCap.caps} and {@code WandRod.rods} by tag
 * ({@code api/wands/WandCap.java:23}, {@code WandRod.java:18}) and the
 * {@code StaffRod} constructor derived its own key by suffixing the base tag
 * with {@code "_staff"} ({@code StaffRod.java:10}). docs/content-mapping.csv
 * therefore carries <em>two</em> {@code wand_component} rows under one id for
 * every rod that has a staff core (for example {@code Thaumcraft:greatwood}
 * as both {@code wand_rod} and {@code staff_rod}); the shared-id group is
 * documented in {@code ContentMappingIdContractTest.SHARED_ID_ALLOWLIST}, and
 * {@code RegistrationGameTest} resolves a {@code wand_component} row by that
 * id — so the registry holds one entry per <em>distinct</em> tag, and an entry
 * carries the rod-only, staff-only or both variants registered under it.</p>
 *
 * @param tag      the component tag (the {@code new_id} path of the CSV rows)
 * @param cap      the cap registered with this tag, or null
 * @param wandRod  the wand rod registered with this tag, or null
 * @param staffRod the staff core registered with this tag, or null
 */
public record WandComponent(String tag, WandCap cap, WandRod wandRod, WandRod staffRod) {

    /** Every rod variant registered under this tag (0, 1 or 2 entries). */
    public List<WandRod> rods() {
        final List<WandRod> out = new ArrayList<>(2);
        if (this.wandRod != null) {
            out.add(this.wandRod);
        }
        if (this.staffRod != null) {
            out.add(this.staffRod);
        }
        return out;
    }

    /** Rows in docs/content-mapping.csv this single registry entry backs. */
    public int componentRowCount() {
        return (this.cap == null ? 0 : 1) + this.rods().size();
    }
}
