package net.runelite.client.plugins.notes.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.events.Event;

@AllArgsConstructor
public class PageAdded implements Event
{
	@Getter
	@Setter
	private int index;
}
