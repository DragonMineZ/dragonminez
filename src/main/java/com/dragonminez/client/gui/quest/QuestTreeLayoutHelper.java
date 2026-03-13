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

		// --- Layout main saga quests horizontally on a single row ---
		int maxPixelX = 0;
		int maxPixelY = 0;
		int minPixelY = 0;

		for (int i = 0; i < sagaQuests.size(); i++) {
			Quest quest = sagaQuests.get(i);
			int row = 0;

			int pixelX = i * NODE_SPACING_X;
			int pixelY = row * NODE_SPACING_Y;

			NodePosition node = new NodePosition(quest, i, row, pixelX, pixelY, false);
			nodes.add(node);
			sagaNodeMap.put(quest.getId(), node);

			maxPixelX = Math.max(maxPixelX, pixelX);
			maxPixelY = Math.max(maxPixelY, pixelY);
			minPixelY = Math.min(minPixelY, pixelY);
		}

		// Build saga connections based on prerequisites first, then fallback to previous quest in file order
		for (int i = 0; i < sagaQuests.size(); i++) {
			Quest child = sagaQuests.get(i);
			NodePosition to = sagaNodeMap.get(child.getId());
			if (to == null) continue;

			NodePosition from = findSagaParentNode(child, saga.getId(), sagaNodeMap, sagaQuests, i);
			if (from != null) {
				connections.add(new NodeConnection(from, to));
			}
		}

		// --- Attach side-quests as up/down diagonal branches ---
		Map<String, Quest> allQuests = QuestRegistry.getClientQuests();
		if (!allQuests.isEmpty()) {
			Map<String, NodePosition> sideNodeMap = new HashMap<>();
			Map<String, Integer> sideDirectionByQuestKey = new HashMap<>();
			Map<String, Integer> nextAnchorDirection = new HashMap<>();
			Set<String> occupied = new HashSet<>();
			for (NodePosition n : nodes) {
				occupied.add(n.getGridCol() + ":" + n.getGridRow());
			}

			List<Quest> sortedSide = new ArrayList<>();
			for (Quest q : allQuests.values()) {
				if (q.isSideQuest() && belongsToSaga(q, saga.getId())) {
					sortedSide.add(q);
				}
			}
			sortedSide.sort(Comparator.comparing(q -> q.getStringId() != null ? q.getStringId() : ""));

			for (Quest sq : sortedSide) {
				NodePosition parent = findParentNode(sq, saga.getId(), sagaNodeMap, sideNodeMap);
				int attachCol = findAttachColumn(sq, saga.getId(), sagaNodeMap, sideNodeMap);
				if (attachCol < 0) {
					attachCol = 0;
				}

				String anchorKey = parent != null ? questKey(parent.getQuest()) : "saga:" + attachCol;
				int direction;
				if (parent != null && parent.isSidequest()) {
					direction = sideDirectionByQuestKey.getOrDefault(anchorKey, 1);
				} else {
					direction = nextAnchorDirection.getOrDefault(anchorKey, 1);
					nextAnchorDirection.put(anchorKey, -direction);
				}

				int col;
				int row;
				if (parent != null) {
					col = parent.getGridCol() + 1;
					row = parent.getGridRow() + direction;
				} else {
					NodePosition sagaAnchor = findSagaNodeByColumn(sagaNodeMap, attachCol);
					int baseRow = sagaAnchor != null ? sagaAnchor.getGridRow() : 0;
					col = attachCol + 1;
					row = baseRow + direction;
				}

				row = reserveClosestFreeRow(occupied, col, row, direction);

				int pixelX = col * NODE_SPACING_X;
				int pixelY = row * NODE_SPACING_Y;
				NodePosition sideNode = new NodePosition(sq, col, row, pixelX, pixelY, true);
				nodes.add(sideNode);
				occupied.add(col + ":" + row);

				String key = questKey(sq);
				if (key != null) {
					sideNodeMap.put(key, sideNode);
					sideDirectionByQuestKey.put(key, direction);
				}

				maxPixelX = Math.max(maxPixelX, pixelX);
				maxPixelY = Math.max(maxPixelY, pixelY);
				minPixelY = Math.min(minPixelY, pixelY);

				if (parent != null) {
					connections.add(new NodeConnection(parent, sideNode));
				}
			}
		}

		int totalWidth = maxPixelX + NODE_SPACING_X;
		int totalHeight = (maxPixelY - minPixelY) + NODE_SPACING_Y;

		if (minPixelY < 0) {
			int shift = -minPixelY;
			List<NodePosition> shiftedNodes = new ArrayList<>(nodes.size());
			for (NodePosition node : nodes) {
				shiftedNodes.add(new NodePosition(node.getQuest(), node.getGridCol(), node.getGridRow(),
						node.getPixelX(), node.getPixelY() + shift, node.isSidequest()));
			}
			List<NodeConnection> shiftedConnections = new ArrayList<>(connections.size());
			for (NodeConnection connection : connections) {
				NodePosition shiftedFrom = findShiftedNode(shiftedNodes, connection.getFrom());
				NodePosition shiftedTo = findShiftedNode(shiftedNodes, connection.getTo());
				if (shiftedFrom != null && shiftedTo != null) {
					shiftedConnections.add(new NodeConnection(shiftedFrom, shiftedTo));
				}
			}
			return new TreeLayout(shiftedNodes, shiftedConnections, totalWidth, totalHeight);
		}

		return new TreeLayout(nodes, connections, totalWidth, totalHeight);
	}

	private static NodePosition findShiftedNode(List<NodePosition> nodes, NodePosition original) {
		for (NodePosition node : nodes) {
			if (node.getQuest() == original.getQuest()) {
				return node;
			}
		}
		return null;
	}

	private static NodePosition findSagaParentNode(Quest child, String sagaId,
									   Map<Integer, NodePosition> sagaNodeMap,
									   List<Quest> sagaQuests,
									   int childIndex) {
		if (child.getPrerequisites() != null && child.getPrerequisites().getConditions() != null) {
			for (var cond : child.getPrerequisites().getConditions()) {
				if (cond.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST
						&& sagaId.equals(cond.getSagaId())
						&& cond.getQuestId() != null) {
					NodePosition parent = sagaNodeMap.get(cond.getQuestId());
					if (parent != null) return parent;
				}
			}
		}

		if (childIndex > 0) {
			Quest previous = sagaQuests.get(childIndex - 1);
			return sagaNodeMap.get(previous.getId());
		}
		return null;
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

	private static NodePosition findSagaNodeByColumn(Map<Integer, NodePosition> sagaNodeMap, int column) {
		for (NodePosition node : sagaNodeMap.values()) {
			if (node.getGridCol() == column) {
				return node;
			}
		}
		return null;
	}

	private static int reserveClosestFreeRow(Set<String> occupied, int col, int startRow, int direction) {
		int row = startRow;
		while (occupied.contains(col + ":" + row)) {
			row += direction;
		}
		return row;
	}

	private static String questKey(Quest quest) {
		if (quest == null) return null;
		if (quest.getStringId() != null) return quest.getStringId();
		if (quest.getId() >= 0) return String.valueOf(quest.getId());
		return null;
	}
}
