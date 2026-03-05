package com.dragonminez.client.gui.quest;

import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import lombok.Getter;

import java.util.*;

/**
 * Computes pixel positions for quests within a saga to display them as a
 * horizontal left-to-right tree.
 * <p>
 * The main saga quests form a straight horizontal line going right.
 * Side-quests that are associated with a saga (via prerequisites) branch
 * off vertically from the relevant saga quest node.
 * Side-quests with no direct saga prerequisite float nearby the first node.
 *
 * @since 2.1
 */
public class QuestTreeLayoutHelper {

	/** Spacing in pixels between node centers horizontally. */
	public static final int NODE_SPACING_X = 55;
	/** Spacing in pixels between node centers vertically (for branches). */
	public static final int NODE_SPACING_Y = 45;

	/**
	 * Computed position for a single quest node in the tree.
	 */
	@Getter
	public static class NodePosition {
		private final Quest quest;
		private final int gridCol;
		private final int gridRow;
		private final int pixelX;
		private final int pixelY;
		/** True if this node represents a sidequest rather than a main saga quest. */
		private final boolean sidequest;

		public NodePosition(Quest quest, int gridCol, int gridRow, int pixelX, int pixelY, boolean sidequest) {
			this.quest = quest;
			this.gridCol = gridCol;
			this.gridRow = gridRow;
			this.pixelX = pixelX;
			this.pixelY = pixelY;
			this.sidequest = sidequest;
		}
	}

	/**
	 * Represents a connection (edge) between two quest nodes.
	 */
	@Getter
	public static class NodeConnection {
		private final NodePosition from;
		private final NodePosition to;

		public NodeConnection(NodePosition from, NodePosition to) {
			this.from = from;
			this.to = to;
		}
	}

	/**
	 * Complete layout result for a saga.
	 */
	@Getter
	public static class TreeLayout {
		private final List<NodePosition> nodes;
		private final List<NodeConnection> connections;
		private final int totalWidth;
		private final int totalHeight;

		public TreeLayout(List<NodePosition> nodes, List<NodeConnection> connections, int totalWidth, int totalHeight) {
			this.nodes = nodes;
			this.connections = connections;
			this.totalWidth = totalWidth;
			this.totalHeight = totalHeight;
		}
	}

	/**
	 * Computes a horizontal left-to-right layout for a saga's quests.
	 * <p>
	 * Main saga quests are placed in a straight horizontal line (row 0).
	 * Side-quests with a prerequisite referencing a saga quest branch off
	 * vertically below the matching saga node. Side-quests without a direct
	 * saga link float near the first node.
	 * <pre>
	 *   [1] → [2] → [3] → [4] → [5] → ...
	 *          |             |
	 *        [SQ1]         [SQ2]
	 *                        |
	 *                      [SQ3]
	 * </pre>
	 *
	 * @param saga the saga to layout
	 * @return the computed layout with nodes and connections
	 */
	public static TreeLayout computeLayout(Saga saga) {
		if (saga == null || saga.getQuests() == null || saga.getQuests().isEmpty()) {
			return new TreeLayout(Collections.emptyList(), Collections.emptyList(), 0, 0);
		}

		List<Quest> sagaQuests = saga.getQuests();
		List<NodePosition> nodes = new ArrayList<>();
		List<NodeConnection> connections = new ArrayList<>();

		// Map saga quest id -> its NodePosition for branch attachment
		Map<Integer, NodePosition> sagaNodeMap = new HashMap<>();

		// --- Layout main saga quests horizontally (row 0) ---
		int maxPixelX = 0;
		int maxPixelY = 0;

		for (int i = 0; i < sagaQuests.size(); i++) {
			Quest quest = sagaQuests.get(i);

			int pixelX = i * NODE_SPACING_X;
			int pixelY = 0; // row 0 = main saga line

			NodePosition node = new NodePosition(quest, i, 0, pixelX, pixelY, false);
			nodes.add(node);
			sagaNodeMap.put(quest.getId(), node);

			maxPixelX = Math.max(maxPixelX, pixelX);
		}

		// Build connections between consecutive saga quests
		for (int i = 0; i < sagaQuests.size() - 1; i++) {
			NodePosition from = sagaNodeMap.get(sagaQuests.get(i).getId());
			NodePosition to = sagaNodeMap.get(sagaQuests.get(i + 1).getId());
			if (from != null && to != null) {
				connections.add(new NodeConnection(from, to));
			}
		}

		// --- Attach side-quests as branches ---
		Map<String, Quest> allQuests = QuestRegistry.getClientQuests();
		if (allQuests != null && !allQuests.isEmpty()) {
			// Track how many sidequests branch from each saga quest column
			Map<Integer, Integer> branchCount = new HashMap<>();
			// Track sidequest chains (sidequest stringId -> its NodePosition)
			Map<String, NodePosition> sideNodeMap = new HashMap<>();

			// Filter to only sidequests that belong to THIS saga (prereq references this saga)
			List<Quest> sortedSide = new ArrayList<>();
			for (Quest q : allQuests.values()) {
				if (q.isSideQuest() && belongsToSaga(q, saga.getId())) {
					sortedSide.add(q);
				}
			}
			sortedSide.sort(Comparator.comparing(q -> q.getStringId() != null ? q.getStringId() : ""));

			for (Quest sq : sortedSide) {
				// Find which saga quest this sidequest is attached to
				int attachCol = findAttachColumn(sq, saga.getId(), sagaNodeMap, sideNodeMap);
				if (attachCol < 0) {
					// No direct link — float near the first node
					attachCol = 0;
				}

				int branchIdx = branchCount.getOrDefault(attachCol, 0) + 1;
				branchCount.put(attachCol, branchIdx);

				int pixelX = attachCol * NODE_SPACING_X;
				int pixelY = branchIdx * NODE_SPACING_Y;

				NodePosition sideNode = new NodePosition(sq, attachCol, branchIdx, pixelX, pixelY, true);
				nodes.add(sideNode);
				if (sq.getStringId() != null) {
					sideNodeMap.put(sq.getStringId(), sideNode);
				}

				maxPixelX = Math.max(maxPixelX, pixelX);
				maxPixelY = Math.max(maxPixelY, pixelY);

				// Connect to parent: either a saga node or another sidequest
				NodePosition parent = findParentNode(sq, saga.getId(), sagaNodeMap, sideNodeMap);
				if (parent != null) {
					connections.add(new NodeConnection(parent, sideNode));
				}
			}
		}

		int totalWidth = maxPixelX + NODE_SPACING_X;
		int totalHeight = maxPixelY + NODE_SPACING_Y;

		return new TreeLayout(nodes, connections, totalWidth, totalHeight);
	}

	/**
	 * Determines which saga column a sidequest should attach to based on its prerequisites.
	 */
	private static int findAttachColumn(Quest sidequest, String sagaId,
										Map<Integer, NodePosition> sagaNodeMap,
										Map<String, NodePosition> sideNodeMap) {
		if (sidequest.getPrerequisites() == null) return -1;

		var conditions = sidequest.getPrerequisites().getConditions();
		if (conditions == null) return -1;

		for (var cond : conditions) {
			// If it references a saga quest directly
			if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST
					&& sagaId.equals(cond.getSagaId())) {
				Integer questId = cond.getQuestId();
				if (questId != null) {
					NodePosition sagaNode = sagaNodeMap.get(questId);
					if (sagaNode != null) return sagaNode.getGridCol();
				}
			}
			// If it references another sidequest
			if (cond.getType() == QuestPrerequisites.ConditionType.QUEST) {
				String refQuestId = cond.getRequiredQuestId();
				if (refQuestId != null) {
					NodePosition parentSide = sideNodeMap.get(refQuestId);
					if (parentSide != null) return parentSide.getGridCol();
				}
			}
		}
		return -1;
	}

	/**
	 * Finds the parent node for a sidequest (for drawing the connection line).
	 */
	private static NodePosition findParentNode(Quest sidequest, String sagaId,
											   Map<Integer, NodePosition> sagaNodeMap,
											   Map<String, NodePosition> sideNodeMap) {
		if (sidequest.getPrerequisites() == null) return null;

		var conditions = sidequest.getPrerequisites().getConditions();
		if (conditions == null) return null;

		for (var cond : conditions) {
			// Prefer sidequest parent first (for chains)
			if (cond.getType() == QuestPrerequisites.ConditionType.QUEST) {
				String refQuestId = cond.getRequiredQuestId();
				if (refQuestId != null) {
					NodePosition parentSide = sideNodeMap.get(refQuestId);
					if (parentSide != null) return parentSide;
				}
			}
		}
		for (var cond : conditions) {
			// Then saga quest parent
			if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST
					&& sagaId.equals(cond.getSagaId())) {
				Integer questId = cond.getQuestId();
				if (questId != null) {
					return sagaNodeMap.get(questId);
				}
			}
		}
		return null;
	}

	/**
	 * Checks whether a sidequest belongs to a specific saga by inspecting its prerequisites.
	 * A sidequest belongs to a saga if it has a SAGA_QUEST condition referencing that saga,
	 * or if it chains off another sidequest that belongs to that saga.
	 */
	private static boolean belongsToSaga(Quest sidequest, String sagaId) {
		if (sidequest.getPrerequisites() == null) return false;

		var conditions = sidequest.getPrerequisites().getConditions();
		if (conditions == null) return false;

		for (var cond : conditions) {
			if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST
					&& sagaId.equals(cond.getSagaId())) {
				return true;
			}
			// If it references another sidequest, check if that sidequest belongs to this saga
			if (cond.getType() == QuestPrerequisites.ConditionType.QUEST) {
				String refId = cond.getRequiredQuestId();
				if (refId != null) {
					Quest parent = QuestRegistry.getClientQuest(refId);
					if (parent != null && parent.isSideQuest() && belongsToSaga(parent, sagaId)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
