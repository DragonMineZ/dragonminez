package com.dragonminez.common.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

public final class PartyFeedback {

	private PartyFeedback() {}

	public static void inviteRequest(ServerPlayer inviter, ServerPlayer invitee, PartyManager.InviteRequestResult result) {
		switch (result) {
			case INVITED -> {
				inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.sent", invitee.getGameProfile().getName()));
				sendInviteReceived(inviter, invitee);
			}
			case SUGGESTED -> {
				ServerPlayer leader = PartyManager.getPartyLeader(inviter);
				inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.suggested",
						invitee.getGameProfile().getName(), leader != null ? leader.getGameProfile().getName() : "?"));
			}
			case CANNOT_INVITE_SELF -> inviter.sendSystemMessage(red("quest.dmz.party.invite.self"));
			case ALREADY_IN_PARTY -> inviter.sendSystemMessage(red("quest.dmz.party.invite.same_party"));
			case NO_PERMISSION -> inviter.sendSystemMessage(red("quest.dmz.party.invite.leader_offline"));
			case TOURNAMENT_ACTIVE -> inviter.sendSystemMessage(red("tournament.dragonminez.party_locked"));
			case PARTY_FULL -> inviter.sendSystemMessage(red("quest.dmz.party.invite.party_full_inviter",
					PartyManager.getAllPartyMembers(inviter).size(), PartyManager.maxMembers()));
			case LEVEL_GAP -> {
				ServerPlayer leader = resolvedLeader(inviter);
				String inviteeName = invitee.getGameProfile().getName();
				int leaderLevel = PartyManager.levelOf(leader);
				int inviteeLevel = PartyManager.levelOf(invitee);
				int gap = PartyManager.maxLevelGap();
				inviter.sendSystemMessage(red("quest.dmz.party.invite.level_gap.inviter", inviteeName, leaderLevel, inviteeLevel, gap));
				invitee.sendSystemMessage(red("quest.dmz.party.invite.level_gap.invitee",
						inviter.getGameProfile().getName(), leaderLevel, inviteeLevel, gap));
			}
			case DIFFICULTY_TOO_LOW -> {
				ServerPlayer leader = resolvedLeader(inviter);
				Component partyDifficulty = difficultyName(PartyManager.difficultyOf(leader));
				Component inviteeDifficulty = difficultyName(PartyManager.difficultyOf(invitee));
				inviter.sendSystemMessage(red("quest.dmz.party.invite.difficulty_low.inviter",
						invitee.getGameProfile().getName(), inviteeDifficulty, partyDifficulty));
				invitee.sendSystemMessage(red("quest.dmz.party.invite.difficulty_low.invitee",
						inviter.getGameProfile().getName(), partyDifficulty, inviteeDifficulty));
			}
		}
	}

	public static void inviteeOffline(ServerPlayer inviter) {
		inviter.sendSystemMessage(red("quest.dmz.party.invite.offline"));
	}

	public static void inviteAccept(ServerPlayer invitee, PartyManager.PendingInvite invite, PartyManager.InviteAcceptResult result) {
		ServerPlayer leader = invite != null && invite.getPartyLeaderId() != null
				? invitee.getServer().getPlayerList().getPlayer(invite.getPartyLeaderId()) : null;
		switch (result) {
			case SUCCESS -> {
				invitee.sendSystemMessage(Component.translatable("quest.dmz.party.joined").withStyle(ChatFormatting.GREEN));
				for (ServerPlayer member : PartyManager.getAllPartyMembers(invitee)) {
					if (member == invitee) continue;
					member.sendSystemMessage(Component.translatable("quest.dmz.party.player.joined", invitee.getName()).withStyle(ChatFormatting.GREEN));
				}
			}
			case EXPIRED -> invitee.sendSystemMessage(red("quest.dmz.party.invite.expired"));
			case TOURNAMENT_ACTIVE -> invitee.sendSystemMessage(red("tournament.dragonminez.party_locked"));
			case LEADER_OFFLINE -> invitee.sendSystemMessage(red("quest.dmz.party.accept.leader_offline"));
			case PARTY_DISBANDED -> invitee.sendSystemMessage(red("quest.dmz.party.accept.party_gone"));
			case PARTY_FULL -> {
				int members = leader != null ? PartyManager.getAllPartyMembers(leader).size() : PartyManager.maxMembers();
				invitee.sendSystemMessage(red("quest.dmz.party.accept.party_full", members, PartyManager.maxMembers()));
				if (leader != null) leader.sendSystemMessage(red("quest.dmz.party.accept.party_full.leader",
						invitee.getGameProfile().getName(), members, PartyManager.maxMembers()));
			}
			case LEVEL_GAP -> {
				int leaderLevel = PartyManager.levelOf(leader);
				int inviteeLevel = PartyManager.levelOf(invitee);
				int gap = PartyManager.maxLevelGap();
				String leaderName = leader != null ? leader.getGameProfile().getName() : invite.getInviterName();
				invitee.sendSystemMessage(red("quest.dmz.party.accept.level_gap", leaderName, leaderLevel, inviteeLevel, gap));
				if (leader != null) leader.sendSystemMessage(red("quest.dmz.party.accept.level_gap.leader",
						invitee.getGameProfile().getName(), leaderLevel, inviteeLevel, gap));
			}
			case DIFFICULTY_TOO_LOW -> {
				Component partyDifficulty = difficultyName(leader != null ? PartyManager.difficultyOf(leader) : invite.getPartyDifficulty());
				Component ownDifficulty = difficultyName(PartyManager.difficultyOf(invitee));
				invitee.sendSystemMessage(red("quest.dmz.party.accept.difficulty_low", partyDifficulty, ownDifficulty));
				if (leader != null) leader.sendSystemMessage(red("quest.dmz.party.accept.difficulty_low.leader",
						invitee.getGameProfile().getName(), ownDifficulty, partyDifficulty));
			}
			case DIFFICULTY_CONFIRM_REQUIRED -> {
				Component partyDifficulty = difficultyName(leader != null ? PartyManager.difficultyOf(leader) : invite.getPartyDifficulty());
				Component ownDifficulty = difficultyName(PartyManager.difficultyOf(invitee));
				UUID partyId = invite != null ? invite.getPartyId() : null;
				String command = "/dmzparty accept confirm" + (partyId != null ? " " + partyId : "");
				Component confirmButton = Component.translatable("quest.dmz.party.invite.difficulty_confirm.button")
						.withStyle(style -> style
								.withColor(ChatFormatting.GREEN)
								.withBold(true)
								.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
								.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										Component.translatable("quest.dmz.party.invite.difficulty_confirm.hover"))));
				invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.difficulty_confirm.detail", partyDifficulty, ownDifficulty)
						.withStyle(ChatFormatting.YELLOW));
				invitee.sendSystemMessage(Component.literal("[").append(confirmButton).append(Component.literal("]")));
			}
			case INVALID -> invitee.sendSystemMessage(red("quest.dmz.party.invite.invalid"));
		}
	}

	private static void sendInviteReceived(ServerPlayer inviter, ServerPlayer invitee) {
		UUID partyId = PartyManager.getPartyId(inviter);
		String suffix = partyId != null ? " " + partyId : "";
		invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.received", inviter.getName()));

		ServerPlayer leader = resolvedLeader(inviter);
		Difficulty partyDifficulty = PartyManager.difficultyOf(leader);
		Difficulty ownDifficulty = PartyManager.difficultyOf(invitee);
		if (partyDifficulty.ordinal() > ownDifficulty.ordinal()) {
			invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.difficulty_higher",
					difficultyName(partyDifficulty), difficultyName(ownDifficulty)).withStyle(ChatFormatting.YELLOW));
		}

		Component acceptButton = Component.translatable("quest.dmz.party.invite.accept")
				.withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzparty accept" + suffix))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								Component.translatable("quest.dmz.party.invite.accept.hover"))));
		Component rejectButton = Component.translatable("quest.dmz.party.invite.reject")
				.withStyle(style -> style
						.withColor(ChatFormatting.RED)
						.withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzparty reject" + suffix))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								Component.translatable("quest.dmz.party.invite.reject.hover"))));
		invitee.sendSystemMessage(Component.literal("[").append(acceptButton).append(Component.literal("] ["))
				.append(rejectButton).append(Component.literal("]")));
	}

	private static ServerPlayer resolvedLeader(ServerPlayer inviter) {
		ServerPlayer leader = PartyManager.isInParty(inviter) ? PartyManager.getPartyLeader(inviter) : inviter;
		return leader != null ? leader : inviter;
	}

	public static Component difficultyName(Difficulty difficulty) {
		Difficulty value = difficulty != null ? difficulty : Difficulty.NORMAL;
		return Component.translatable("gui.dragonminez.quest_tree.difficulty." + value.name().toLowerCase(Locale.ROOT));
	}

	private static MutableComponent red(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.RED);
	}
}
