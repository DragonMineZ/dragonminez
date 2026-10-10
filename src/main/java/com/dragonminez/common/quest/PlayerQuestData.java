package com.dragonminez.common.quest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.rewards.AttributePointsReward;
import com.dragonminez.common.quest.rewards.AttributesReward;
import com.dragonminez.common.quest.rewards.GenericItemReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerQuestData {

    public enum QuestStatus {
        NOT_STARTED,
        ACCEPTED,
        FAILED,
        SUCCESS
    }
    private final Map<String, QuestProgress> quests = new LinkedHashMap<>();
    private final Map<String, Boolean> sagaUnlockState = new HashMap<>();
    private final Map<String, QuestStartRequirementTiming> startRequirementTimings = new LinkedHashMap<>();
    private final Set<String> hostileNpcKeys = new LinkedHashSet<>();
    @Getter
    private String trackedQuestId = null;
    @Getter
    private Difficulty difficulty = Difficulty.NORMAL;
    @Getter
    @Setter
    private boolean difficultyChosen = false;
    @Getter
    private int storyResetCount = 0;
    @Getter
    private UUID activePartyId = null;

    @Getter
    private UUID partyLeaderId = null;

    @Getter
    private UUID partyOwnerId = null;

    private final List<UUID> partyMemberIds = new ArrayList<>();

    @Getter
    private boolean partyPvpEnabled = false;

    private final List<PartyInviteData> pendingPartyInvites = new ArrayList<>();

    public static final int MAX_PENDING_PARTY_INVITES = 5;

    @Getter
    @Setter
    private String migratedFor = null;

    public void acceptQuest(String questId) {
        getOrCreateProgress(questId).setStatus(QuestStatus.ACCEPTED);
        clearStartRequirementTiming(questId);
    }

    public void failQuest(String questId) {
        QuestProgress progress = getOrCreateProgress(questId);
        progress.markFailed();
        progress.resetForRestart();
        progress.setStatus(QuestStatus.FAILED);
        clearStartRequirementTiming(questId);
    }

    public void restartFailedQuest(String questId) {
        QuestProgress progress = getOrCreateProgress(questId);
        progress.resetForRestart();
        progress.setStatus(QuestStatus.ACCEPTED);
        clearStartRequirementTiming(questId);
    }

    public void completeQuest(String questId) {
        QuestProgress progress = getOrCreateProgress(questId);
        progress.setStatus(QuestStatus.SUCCESS);
        progress.recordCompletion(System.currentTimeMillis());
        clearStartRequirementTiming(questId);
    }

    public void restartCompletedQuest(String questId) {
        QuestProgress progress = getOrCreateProgress(questId);
        progress.resetForRestart();
        progress.setStatus(QuestStatus.ACCEPTED);
        clearStartRequirementTiming(questId);
    }

    public long getLastCompletedRealMs(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getLastCompletedRealMs() : 0L;
    }

    public int getTimesCompleted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getTimesCompleted() : 0;
    }

    public long getQuestAcceptedGameTime(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getAcceptedGameTime() : -1L;
    }

    public void setQuestAcceptedGameTime(String questId, long gameTime) {
        getOrCreateProgress(questId).setAcceptedGameTime(gameTime);
    }

    public boolean isQuestAccepted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.ACCEPTED;
    }

    public boolean isQuestCompleted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.SUCCESS;
    }

    public QuestStatus getQuestStatus(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;
    }

    public Set<String> getFailedQuestIds() {
        Set<String> failed = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.FAILED) {
                failed.add(entry.getKey());
            }
        }
        return failed;
    }
    public void resetQuest(String questId) {
        quests.remove(questId);
        clearStartRequirementTiming(questId);
    }
    public void setDifficulty(Difficulty newDifficulty) {
        if (newDifficulty == null || newDifficulty == difficulty) return;
        difficulty = newDifficulty;
    }
    public void requestDifficultyReselect() {
        this.difficultyChosen = false;
    }
    public void resetAll() {
        clearActiveQuestState();
    }
    public void markStoryReset() {
        storyResetCount++;
    }

    public void clearStoryResets() {
        storyResetCount = 0;
    }

    public double tpRewardMultiplier() {
        if (storyResetCount <= 0) return 1.0;
        double perReset = ConfigManager.getServerConfig().getGameplay().getStoryResetTPMultiplier();
        return Math.pow(perReset, storyResetCount);
    }

    public double rewardMultiplierFor(QuestReward reward) {
        return rewardMultiplierFor(reward, difficulty);
    }

    public double rewardMultiplierFor(QuestReward reward, Difficulty rewardDifficulty) {
        double multiplier = rewardDifficulty.questRewardMultiplier();
        boolean scaledByResets = reward instanceof TPSReward || reward instanceof AttributesReward
                || reward instanceof AttributePointsReward || reward instanceof ItemReward || reward instanceof GenericItemReward;
        return scaledByResets ? multiplier * tpRewardMultiplier() : multiplier;
    }

    private void clearActiveQuestState() {
        quests.clear();
        sagaUnlockState.clear();
        startRequirementTimings.clear();
        hostileNpcKeys.clear();
        trackedQuestId = null;
    }

    public void resetSaga(String sagaId) {
        String prefix = sagaId + ":";
        quests.keySet().removeIf(key -> key.startsWith(prefix));
        startRequirementTimings.keySet().removeIf(key -> key.startsWith(prefix));
        if (trackedQuestId != null && trackedQuestId.startsWith(prefix)) trackedQuestId = null;
    }

    public void setTrackedQuestId(String trackedQuestId) {
        if (trackedQuestId == null || trackedQuestId.isBlank()) {
            this.trackedQuestId = null;
            return;
        }
        this.trackedQuestId = trackedQuestId;
    }

    public QuestStartRequirementTiming getStartRequirementTiming(String questId) {
        return questId == null ? null : startRequirementTimings.get(questId);
    }

    public boolean ensureStartRequirementTiming(String questId, long gameTimeStarted, long realTimeStartedMs) {
        if (questId == null || questId.isBlank()) return false;
        if (startRequirementTimings.containsKey(questId)) return false;
        startRequirementTimings.put(questId, new QuestStartRequirementTiming(gameTimeStarted, realTimeStartedMs));
        return true;
    }

    public void clearStartRequirementTiming(String questId) {
        if (questId == null || questId.isBlank()) return;
        startRequirementTimings.remove(questId);
    }

    public void markNpcHostile(String npcKey) {
        if (npcKey == null || npcKey.isBlank()) return;
        hostileNpcKeys.add(npcKey);
    }

    public boolean isNpcHostile(String npcKey) {
        return npcKey != null && hostileNpcKeys.contains(npcKey);
    }

    public void clearNpcHostility(String npcKey) {
        if (npcKey == null || npcKey.isBlank()) return;
        hostileNpcKeys.remove(npcKey);
    }

    public Set<String> getAcceptedQuestIds() {
        Set<String> accepted = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.ACCEPTED) {
                accepted.add(entry.getKey());
            }
        }
        return accepted;
    }

    public Set<String> getCompletedQuestIds() {
        Set<String> completed = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.SUCCESS) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }

    // ========================================================================================
    // Objective Progress
    // ========================================================================================

    public void setObjectiveProgress(String questId, int objectiveIndex, int progress) {
        getOrCreateProgress(questId).setObjectiveProgress(objectiveIndex, progress);
    }

    public void setObjectiveRequired(String questId, int objectiveIndex, int required) {
        getOrCreateProgress(questId).setObjectiveRequired(objectiveIndex, required);
    }

    public int getObjectiveRequired(String questId, int objectiveIndex, int fallbackRequired) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getObjectiveRequired(objectiveIndex, fallbackRequired) : fallbackRequired;
    }

    public void setQuestDifficulty(String questId, Difficulty difficulty) {
        getOrCreateProgress(questId).setDifficulty(difficulty != null ? difficulty : Difficulty.NORMAL);
    }

    public Difficulty getQuestDifficulty(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getDifficulty() : Difficulty.NORMAL;
    }

    public int getQuestFailureCount(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getFailureCount() : 0;
    }

    public int getObjectiveProgress(String questId, int objectiveIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getObjectiveProgress(objectiveIndex) : 0;
    }

    // ========================================================================================
    // Reward Claims
    // ========================================================================================

    public void claimReward(String questId, int rewardIndex) {
        getOrCreateProgress(questId).claimReward(rewardIndex);
    }

    public boolean isRewardClaimed(String questId, int rewardIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.isRewardClaimed(rewardIndex);
    }

    public boolean hasAnyRewardClaim(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.rewardsClaimed.containsValue(Boolean.TRUE);
    }

    public void claimAllRewardSlots(String questId, int rewardCount) {
        QuestProgress progress = getOrCreateProgress(questId);
        for (int i = 0; i < rewardCount; i++) progress.claimReward(i);
    }

    public void completeWithoutRewards(String questId, int rewardCount) {
        QuestProgress progress = getOrCreateProgress(questId);
        progress.setStatus(QuestStatus.SUCCESS);
        progress.recordCompletion(System.currentTimeMillis());
        for (int i = 0; i < rewardCount; i++) progress.claimReward(i);
        clearStartRequirementTiming(questId);
    }

    public int getHighestClaimedRewardIndex(String questId) {
        QuestProgress progress = quests.get(questId);
        int highest = -1;
        if (progress == null) return highest;
        for (Map.Entry<Integer, Boolean> entry : progress.rewardsClaimed.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) highest = Math.max(highest, entry.getKey());
        }
        return highest;
    }

    public Map<Integer, Integer> getObjectiveRequiredSnapshot(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? new HashMap<>(progress.objectiveRequired) : new HashMap<>();
    }

    public void resetObjectiveProgress(String questId) {
        QuestProgress progress = quests.get(questId);
        if (progress != null) progress.objectiveProgress.clear();
    }

    public String getAcceptedDimension(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getAcceptedDimension() : null;
    }

    public void setAcceptedDimension(String questId, String dimension) {
        getOrCreateProgress(questId).setAcceptedDimension(dimension);
    }

    public String getObjectiveSignature(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getObjectiveSignature() : null;
    }

    public void setObjectiveSignature(String questId, String signature) {
        getOrCreateProgress(questId).setObjectiveSignature(signature);
    }

    public Set<String> getKnownQuestIds() {
        return new LinkedHashSet<>(quests.keySet());
    }

    // ========================================================================================
    // Saga Unlock State
    // ========================================================================================

    public void setSagaUnlocked(String sagaId, boolean unlocked) {
        sagaUnlockState.put(sagaId, unlocked);
    }

    public boolean isSagaLocked(String sagaId) {
        return sagaUnlockState.getOrDefault(sagaId, false);
    }

    // ========================================================================================
    // Saga Quest Keys
    // ========================================================================================

    public static String sagaQuestKey(String sagaId, int questId) {
        return sagaId + ":" + questId;
    }

    // ========================================================================================
    // Party State
    // ========================================================================================

	public List<UUID> getPartyMemberIds() {
        return Collections.unmodifiableList(partyMemberIds);
    }

    public boolean isInParty() {
        return activePartyId != null;
    }

    public boolean isPartyLeader(UUID playerId) {
        return playerId != null && playerId.equals(partyLeaderId);
    }

    public boolean isPartyOwner(UUID playerId) {
        return playerId != null && playerId.equals(partyOwnerId != null ? partyOwnerId : partyLeaderId);
    }

    public void setPartyState(UUID partyId, UUID leaderId, UUID ownerId, Collection<UUID> members, boolean pvpEnabled) {
        this.activePartyId = partyId;
        this.partyPvpEnabled = pvpEnabled;
        this.partyMemberIds.clear();

        if (members != null) {
            for (UUID memberId : members) {
                if (memberId == null || this.partyMemberIds.contains(memberId)) continue;
                this.partyMemberIds.add(memberId);
            }
        }
        this.partyLeaderId = leaderId != null && partyMemberIds.contains(leaderId) ? leaderId : null;
        this.partyOwnerId = ownerId != null && partyMemberIds.contains(ownerId) ? ownerId : null;
    }

    public void clearPartyState() {
        this.activePartyId = null;
        this.partyLeaderId = null;
        this.partyOwnerId = null;
        this.partyPvpEnabled = false;
        this.partyMemberIds.clear();
    }

    public List<PartyInviteData> getPendingPartyInvites() {
        pendingPartyInvites.removeIf(PartyInviteData::isExpired);
        return Collections.unmodifiableList(pendingPartyInvites);
    }

    public PartyInviteData getPendingPartyInviteData() {
        List<PartyInviteData> invites = getPendingPartyInvites();
        return invites.isEmpty() ? null : invites.get(invites.size() - 1);
    }

    public PartyInviteData getPendingPartyInvite(UUID partyId) {
        if (partyId == null) return getPendingPartyInviteData();
        for (PartyInviteData invite : getPendingPartyInvites()) {
            if (partyId.equals(invite.getPartyId())) return invite;
        }
        return null;
    }

    public boolean hasPendingPartyInvite() {
        return !getPendingPartyInvites().isEmpty();
    }

    public void addPendingPartyInvite(PartyInviteData invite) {
        if (invite == null) return;
        pendingPartyInvites.removeIf(existing -> existing.isExpired()
                || (invite.getPartyId() != null && invite.getPartyId().equals(existing.getPartyId()))
                || (invite.getInviterUUID() != null && invite.getInviterUUID().equals(existing.getInviterUUID())));
        pendingPartyInvites.add(invite);
        while (pendingPartyInvites.size() > MAX_PENDING_PARTY_INVITES) pendingPartyInvites.remove(0);
    }

    public void removePendingPartyInvite(UUID partyId) {
        if (partyId == null) {
            if (!pendingPartyInvites.isEmpty()) pendingPartyInvites.remove(pendingPartyInvites.size() - 1);
            return;
        }
        pendingPartyInvites.removeIf(invite -> partyId.equals(invite.getPartyId()));
    }

	public void clearPendingPartyInvite() {
        this.pendingPartyInvites.clear();
    }

    // ========================================================================================
    // Internal Helpers
    // ========================================================================================

    private QuestProgress getOrCreateProgress(String questId) {
        return quests.computeIfAbsent(questId, QuestProgress::new);
    }

    private CompoundTag serializeCoreQuestState() {
        CompoundTag tag = new CompoundTag();

        ListTag questList = new ListTag();
        for (QuestProgress progress : quests.values()) {
            questList.add(progress.serializeNBT());
        }
        tag.put("quests", questList);

        CompoundTag sagaUnlocks = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : sagaUnlockState.entrySet()) {
            sagaUnlocks.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("sagaUnlocks", sagaUnlocks);

        if (!startRequirementTimings.isEmpty()) {
            CompoundTag timingTag = new CompoundTag();
            for (Map.Entry<String, QuestStartRequirementTiming> entry : startRequirementTimings.entrySet()) {
                timingTag.put(entry.getKey(), entry.getValue().serializeNBT());
            }
            tag.put("startRequirementTimings", timingTag);
        }

        if (trackedQuestId != null && !trackedQuestId.isBlank()) {
            tag.putString("trackedQuestId", trackedQuestId);
        }

        if (!hostileNpcKeys.isEmpty()) {
            ListTag hostileNpcs = new ListTag();
            for (String npcKey : hostileNpcKeys) {
                hostileNpcs.add(StringTag.valueOf(npcKey));
            }
            tag.put("hostileNpcKeys", hostileNpcs);
        }

        return tag;
    }

    private void deserializeCoreQuestState(CompoundTag tag) {
        clearActiveQuestState();

        ListTag questList = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag questTag = questList.getCompound(i);
            QuestProgress progress = QuestProgress.deserialize(questTag);
            quests.put(progress.getQuestId(), progress);
        }

        if (tag.contains("sagaUnlocks")) {
            CompoundTag sagaUnlocks = tag.getCompound("sagaUnlocks");
            for (String key : sagaUnlocks.getAllKeys()) {
                sagaUnlockState.put(key, sagaUnlocks.getBoolean(key));
            }
        }

        if (tag.contains("startRequirementTimings", Tag.TAG_COMPOUND)) {
            CompoundTag timingTag = tag.getCompound("startRequirementTimings");
            for (String key : timingTag.getAllKeys()) {
                if (!timingTag.contains(key, Tag.TAG_COMPOUND)) continue;
                startRequirementTimings.put(key, QuestStartRequirementTiming.deserialize(timingTag.getCompound(key)));
            }
        }

        if (tag.contains("trackedQuestId", Tag.TAG_STRING)) {
            String tracked = tag.getString("trackedQuestId");
            if (!tracked.isBlank()) trackedQuestId = tracked;
        }

        if (tag.contains("hostileNpcKeys", Tag.TAG_LIST)) {
            ListTag hostileNpcs = tag.getList("hostileNpcKeys", Tag.TAG_STRING);
            for (int i = 0; i < hostileNpcs.size(); i++) {
                String npcKey = hostileNpcs.getString(i);
                if (!npcKey.isBlank()) {
                    hostileNpcKeys.add(npcKey);
                }
            }
        }
    }

    private CompoundTag serializeFullQuestState() {
        CompoundTag tag = new CompoundTag();
        tag.putString("difficulty", difficulty.name());
        tag.put("questState", serializeCoreQuestState());
        return tag;
    }

    private void deserializeFullQuestState(CompoundTag tag) {
        clearActiveQuestState();

        if (tag.contains("questState", Tag.TAG_COMPOUND)) {
            difficulty = Difficulty.fromName(tag.getString("difficulty"));
            deserializeCoreQuestState(tag.getCompound("questState"));
        } else if (tag.contains("difficultyStates", Tag.TAG_COMPOUND)) {
            difficulty = Difficulty.fromName(tag.getString("difficulty"));
            CompoundTag states = tag.getCompound("difficultyStates");
            if (states.contains(difficulty.name(), Tag.TAG_COMPOUND)) {
                deserializeCoreQuestState(states.getCompound(difficulty.name()));
            }
        } else {
            difficulty = tag.getBoolean("hardModeEnabled") ? Difficulty.HARD : Difficulty.NORMAL;
            deserializeCoreQuestState(tag);
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // ========================================================================================
    // NBT Serialization
    // ========================================================================================

    public CompoundTag serializeNBT() {
        CompoundTag tag = serializeFullQuestState();
        tag.putBoolean("difficultyChosen", difficultyChosen);
        tag.putInt("storyResetCount", storyResetCount);
        if (migratedFor != null && !migratedFor.isBlank()) {
            tag.putString("migratedFor", migratedFor);
        }

        CompoundTag partyTag = new CompoundTag();
        if (activePartyId != null) {
            partyTag.putString("partyId", activePartyId.toString());
        }
        if (partyLeaderId != null) {
            partyTag.putString("leaderId", partyLeaderId.toString());
        }
        if (partyOwnerId != null) {
            partyTag.putString("ownerId", partyOwnerId.toString());
        }
        if (partyPvpEnabled) {
            partyTag.putBoolean("pvpEnabled", true);
        }
        if (!partyMemberIds.isEmpty()) {
            ListTag membersTag = new ListTag();
            for (UUID memberId : partyMemberIds) {
                membersTag.add(StringTag.valueOf(memberId.toString()));
            }
            partyTag.put("members", membersTag);
        }
        if (!pendingPartyInvites.isEmpty()) {
            ListTag invitesTag = new ListTag();
            for (PartyInviteData invite : pendingPartyInvites) invitesTag.add(invite.serializeNBT());
            partyTag.put("pendingInvites", invitesTag);
        }
        if (!partyTag.isEmpty()) {
            tag.put("partyState", partyTag);
        }

        return tag;
    }
    public void deserializeNBT(CompoundTag tag) {
        deserializeFullQuestState(tag);
        difficultyChosen = tag.getBoolean("difficultyChosen");
        storyResetCount = Math.max(0, tag.getInt("storyResetCount"));
        migratedFor = tag.contains("migratedFor", Tag.TAG_STRING) ? tag.getString("migratedFor") : null;

        activePartyId = null;
        partyLeaderId = null;
        partyOwnerId = null;
        partyPvpEnabled = false;
        partyMemberIds.clear();
        pendingPartyInvites.clear();

        if (tag.contains("partyState", Tag.TAG_COMPOUND)) {
            CompoundTag partyTag = tag.getCompound("partyState");

            if (partyTag.contains("partyId", Tag.TAG_STRING)) {
                activePartyId = parseUuid(partyTag.getString("partyId"));
            }
            if (partyTag.contains("leaderId", Tag.TAG_STRING)) {
                partyLeaderId = parseUuid(partyTag.getString("leaderId"));
            }
            if (partyTag.contains("ownerId", Tag.TAG_STRING)) {
                partyOwnerId = parseUuid(partyTag.getString("ownerId"));
            }
            partyPvpEnabled = partyTag.getBoolean("pvpEnabled");
            if (partyTag.contains("members", Tag.TAG_LIST)) {
                ListTag memberList = partyTag.getList("members", Tag.TAG_STRING);
                for (int i = 0; i < memberList.size(); i++) {
                    UUID memberId = parseUuid(memberList.getString(i));
                    if (memberId != null && !partyMemberIds.contains(memberId)) {
                        partyMemberIds.add(memberId);
                    }
                }
            }
            if (partyTag.contains("pendingInvites", Tag.TAG_LIST)) {
                ListTag invitesTag = partyTag.getList("pendingInvites", Tag.TAG_COMPOUND);
                for (int i = 0; i < invitesTag.size() && pendingPartyInvites.size() < MAX_PENDING_PARTY_INVITES; i++) {
                    pendingPartyInvites.add(PartyInviteData.deserialize(invitesTag.getCompound(i)));
                }
            } else if (partyTag.contains("pendingInvite", Tag.TAG_COMPOUND)) {
                // Saves from before invitations could stack.
                pendingPartyInvites.add(PartyInviteData.deserialize(partyTag.getCompound("pendingInvite")));
            }
        }
    }

    // ========================================================================================
    // Quest Progress Inner Class
    // ========================================================================================

    public static class QuestProgress {

        @Getter
        private final String questId;

        @Setter
        @Getter
        private QuestStatus status;

        private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
        private final Map<Integer, Integer> objectiveRequired = new HashMap<>();
        private final Map<Integer, Boolean> rewardsClaimed = new HashMap<>();
        @Getter
        private int failureCount = 0;
        @Getter
        @Setter
        private Difficulty difficulty = Difficulty.NORMAL;
        @Getter
        private long lastCompletedRealMs = 0L;
        @Getter
        private int timesCompleted = 0;
        @Getter
        @Setter
        private long acceptedGameTime = -1L;
        @Getter
        @Setter
        private String acceptedDimension = null;
        @Getter
        @Setter
        private String objectiveSignature = null;

        public QuestProgress(String questId) {
            this.questId = questId;
            this.status = QuestStatus.NOT_STARTED;
        }

        public void setObjectiveProgress(int index, int progress) {
            objectiveProgress.put(index, progress);
        }

        public int getObjectiveProgress(int index) {
            return objectiveProgress.getOrDefault(index, 0);
        }

        public void setObjectiveRequired(int index, int required) {
            objectiveRequired.put(index, required);
        }

        public int getObjectiveRequired(int index, int fallbackRequired) {
            return objectiveRequired.getOrDefault(index, fallbackRequired);
        }

        public void claimReward(int index) {
            rewardsClaimed.put(index, true);
        }

        public boolean isRewardClaimed(int index) {
            return rewardsClaimed.getOrDefault(index, false);
        }

        public void markFailed() {
            failureCount++;
        }

        public void recordCompletion(long realTimeMs) {
            this.lastCompletedRealMs = realTimeMs;
            this.timesCompleted++;
        }

        public void resetForRestart() {
            objectiveProgress.clear();
            rewardsClaimed.clear();
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("questId", questId);
            tag.putString("status", status.name());

            CompoundTag objectivesTag = new CompoundTag();
            for (Map.Entry<Integer, Integer> entry : objectiveProgress.entrySet()) {
                objectivesTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("objectives", objectivesTag);

            CompoundTag objectiveRequirementsTag = new CompoundTag();
            for (Map.Entry<Integer, Integer> entry : objectiveRequired.entrySet()) {
                objectiveRequirementsTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("objectiveRequirements", objectiveRequirementsTag);
            tag.putInt("failureCount", failureCount);
            tag.putString("difficulty", difficulty.name());
            tag.putLong("lastCompletedRealMs", lastCompletedRealMs);
            tag.putInt("timesCompleted", timesCompleted);
            tag.putLong("acceptedGameTime", acceptedGameTime);
            if (acceptedDimension != null) tag.putString("acceptedDimension", acceptedDimension);
            if (objectiveSignature != null) tag.putString("objectiveSignature", objectiveSignature);

            CompoundTag rewardsTag = new CompoundTag();
            for (Map.Entry<Integer, Boolean> entry : rewardsClaimed.entrySet()) {
                rewardsTag.putBoolean(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("rewards", rewardsTag);

            return tag;
        }

        public static QuestProgress deserialize(CompoundTag tag) {
            String questId = tag.getString("questId");
            QuestProgress progress = new QuestProgress(questId);

            try {
                progress.status = QuestStatus.valueOf(tag.getString("status"));
            } catch (IllegalArgumentException e) {
                progress.status = QuestStatus.NOT_STARTED;
            }

            CompoundTag objectivesTag = tag.getCompound("objectives");
            for (String key : objectivesTag.getAllKeys()) {
                Integer index = parseIndex(key);
                if (index != null) progress.objectiveProgress.put(index, Math.max(0, objectivesTag.getInt(key)));
            }

            CompoundTag objectiveRequirementsTag = tag.getCompound("objectiveRequirements");
            for (String key : objectiveRequirementsTag.getAllKeys()) {
                Integer index = parseIndex(key);
                if (index != null) progress.objectiveRequired.put(index, objectiveRequirementsTag.getInt(key));
            }
            if (tag.contains("acceptedDimension", Tag.TAG_STRING)) {
                progress.acceptedDimension = tag.getString("acceptedDimension");
            }
            if (tag.contains("objectiveSignature", Tag.TAG_STRING)) {
                progress.objectiveSignature = tag.getString("objectiveSignature");
            }
            if (tag.contains("failureCount", Tag.TAG_INT)) {
                progress.failureCount = tag.getInt("failureCount");
            }
            if (tag.contains("lastCompletedRealMs", Tag.TAG_LONG)) {
                progress.lastCompletedRealMs = tag.getLong("lastCompletedRealMs");
            }
            if (tag.contains("timesCompleted", Tag.TAG_INT)) {
                progress.timesCompleted = tag.getInt("timesCompleted");
            }
            if (tag.contains("acceptedGameTime", Tag.TAG_LONG)) {
                progress.acceptedGameTime = tag.getLong("acceptedGameTime");
            } else {
                progress.acceptedGameTime = -1L;
            }
            if (tag.contains("difficulty", Tag.TAG_STRING)) {
                progress.difficulty = Difficulty.fromName(tag.getString("difficulty"));
            } else if (tag.contains("hardMode", Tag.TAG_BYTE)) {
                progress.difficulty = tag.getBoolean("hardMode") ? Difficulty.HARD : Difficulty.NORMAL;
            }

            CompoundTag rewardsTag = tag.getCompound("rewards");
            for (String key : rewardsTag.getAllKeys()) {
                Integer index = parseIndex(key);
                if (index != null) progress.rewardsClaimed.put(index, rewardsTag.getBoolean(key));
            }

            return progress;
        }

        private static Integer parseIndex(String key) {
            try {
                int value = Integer.parseInt(key);
                return value >= 0 ? value : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Getter
    public static class QuestStartRequirementTiming {
        private final long gameTimeStarted;
        private final long realTimeStartedMs;

        public QuestStartRequirementTiming(long gameTimeStarted, long realTimeStartedMs) {
            this.gameTimeStarted = gameTimeStarted;
            this.realTimeStartedMs = realTimeStartedMs;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("gameTimeStarted", gameTimeStarted);
            tag.putLong("realTimeStartedMs", realTimeStartedMs);
            return tag;
        }

        public static QuestStartRequirementTiming deserialize(CompoundTag tag) {
            return new QuestStartRequirementTiming(
                    tag.getLong("gameTimeStarted"),
                    tag.getLong("realTimeStartedMs")
            );
        }
    }

    // ========================================================================================
    // Party Invite Inner Class
    // ========================================================================================

    @Getter
    public static class PartyInviteData {
        private final UUID inviterUUID;
        private final UUID partyId;
        private final UUID partyLeaderId;
        private final String inviterName;
        private final long expiresAtMs;
        private final Difficulty partyDifficulty;

        public PartyInviteData(UUID inviterUUID, UUID partyId, UUID partyLeaderId, String inviterName, long expiresAtMs, Difficulty partyDifficulty) {
            this.inviterUUID = inviterUUID;
            this.partyId = partyId;
            this.partyLeaderId = partyLeaderId;
            this.inviterName = inviterName == null ? "" : inviterName;
            this.expiresAtMs = expiresAtMs;
            this.partyDifficulty = partyDifficulty == null ? Difficulty.NORMAL : partyDifficulty;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (inviterUUID != null) tag.putString("inviterUUID", inviterUUID.toString());
            if (partyId != null) tag.putString("partyId", partyId.toString());
            if (partyLeaderId != null) tag.putString("partyLeaderId", partyLeaderId.toString());
            if (!inviterName.isBlank()) tag.putString("inviterName", inviterName);
            tag.putLong("expiresAtMs", expiresAtMs);
            tag.putString("partyDifficulty", partyDifficulty.name());
            return tag;
        }

        public static PartyInviteData deserialize(CompoundTag tag) {
            return new PartyInviteData(
                    parseUuid(tag.getString("inviterUUID")),
                    parseUuid(tag.getString("partyId")),
                    parseUuid(tag.getString("partyLeaderId")),
                    tag.getString("inviterName"),
                    tag.getLong("expiresAtMs"),
                    Difficulty.fromName(tag.getString("partyDifficulty"))
            );
        }
    }
}
