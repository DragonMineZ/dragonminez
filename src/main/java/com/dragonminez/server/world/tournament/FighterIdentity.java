package com.dragonminez.server.world.tournament;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FighterIdentity {

	private static final List<String> FORM_SUFFIXES = List.of(
			"_base", "_ssj", "_ssj2", "_ssj3", "_ssj4", "_ssg2", "_ssg3", "_lssj", "_restricted",
			"_ultimate", "_fp", "_transformed", "_giant", "_powered", "_noweights", "_baby");

	private static final Map<String, String> ALIASES = new HashMap<>();

	static {
		alias("saga_frieza", "saga_frieza_first", "saga_frieza_second", "saga_frieza_third");
		alias("saga_zarbon", "saga_zarbont1");
		alias("saga_vegeta", "saga_ozaruvegeta");
		alias("saga_cell", "saga_cell_imperfect", "saga_cell_semiperfect", "saga_cell_perfect", "saga_cell_superperfect");
		alias("saga_superbuu", "saga_superbuu_piccolo", "saga_superbuu_gotenks", "saga_superbuu_gohan");
		alias("saga_piccolo_daimao", "saga_piccolo_daimao_old", "saga_piccolo_daimao_young");
		alias("saga_tao_pai_pai", "saga_tao_pai_pai_cyborg");
		alias("saga_cooler", "saga_cooler_5ta");
		alias("saga_metal_cooler", "saga_metal_cooler_core");
		alias("saga_a13", "saga_super_a13");
		alias("saga_janemba", "saga_janemba_fat", "saga_super_janemba");
		alias("saga_hirudegarn", "saga_hirudegarn_incomplete1", "saga_hirudegarn_incomplete2", "saga_super_hirudegarn");
		alias("saga_gogeta_gt", "saga_gogeta_ssj4");
		alias("saga_uub", "saga_majuub");
		alias("saga_baby", "saga_baby_vegeta", "saga_super_baby_vegeta", "saga_super_baby_vegeta2", "saga_baby_golden_ozaru");
		alias("saga_rilldo", "saga_metal_rilldo", "saga_hyper_rilldo");
		alias("saga_omega_shenron", "saga_syn_shenron");
		alias("saga_gomah", "saga_gomah_mini", "saga_gomah_third_eye");
		alias("saga_goku_daima", "saga_goku_mini");
		alias("saga_vegeta_daima", "saga_vegeta_mini");
	}

	private FighterIdentity() {}

	private static void alias(String identity, String... paths) {
		for (String path : paths) ALIASES.put(path, identity);
	}

	public static String of(String entityId) {
		if (entityId == null || entityId.isBlank()) return "";
		int colon = entityId.indexOf(':');
		String namespace = colon >= 0 ? entityId.substring(0, colon + 1) : "";
		String path = colon >= 0 ? entityId.substring(colon + 1) : entityId;

		String aliased = ALIASES.get(path);
		if (aliased != null) return namespace + aliased;

		boolean stripped = true;
		while (stripped) {
			stripped = false;
			for (String suffix : FORM_SUFFIXES) {
				if (path.endsWith(suffix) && path.lastIndexOf('_', path.length() - suffix.length() - 1) >= 0) {
					path = path.substring(0, path.length() - suffix.length());
					stripped = true;
					break;
				}
			}
		}
		aliased = ALIASES.get(path);
		return namespace + (aliased != null ? aliased : path);
	}
}
