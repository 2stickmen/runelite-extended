package net.runelite.rs.api;

import net.runelite.api.ClanMemberManager;
import net.runelite.api.Friend;
import net.runelite.api.Ignore;
import net.runelite.api.NameableContainer;
import net.runelite.mapping.Import;

public interface RSFriendSystem
{
	@Import("friendsList")
	NameableContainer<Friend> getFriendContainer();

	@Import("ignoreList")
	NameableContainer<Ignore> getIgnoreContainer();

	@Import("isFriended")
	boolean isFriended(RSUsername var1, boolean var2);

	@Import("addFriend")
	void addFriend(String username);

	@Import("removeFriend")
	void removeFriend(String username);
}
