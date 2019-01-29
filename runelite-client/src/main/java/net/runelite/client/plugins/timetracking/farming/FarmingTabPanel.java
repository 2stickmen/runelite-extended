/*
 * Copyright (c) 2018 Abex
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.timetracking.farming;

import com.google.common.base.Strings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.timetracking.TabContentPanel;
import net.runelite.client.plugins.timetracking.TimeTrackingConfig;
import net.runelite.client.plugins.timetracking.TimeablePanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

@Slf4j
public class FarmingTabPanel extends TabContentPanel
{
	private final FarmingTracker farmingTracker;
	private final ItemManager itemManager;
	private final TimeTrackingConfig config;
	private final RuneLiteConfig runeLiteConfig;
	private final List<TimeablePanel<FarmingPatch>> patchPanels;

	FarmingTabPanel(
		FarmingTracker farmingTracker,
		ItemManager itemManager,
		RuneLiteConfig runeLiteConfig,
		TimeTrackingConfig config,
		Set<FarmingPatch> patches
	)
	{
		this.farmingTracker = farmingTracker;
		this.itemManager = itemManager;
		this.config = config;
		this.patchPanels = new ArrayList<>();
		this.runeLiteConfig = runeLiteConfig;

		setLayout(new GridBagLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;

		PatchImplementation lastImpl = null;

		boolean first = true;
		for (FarmingPatch patch : patches)
		{
			String title = patch.getRegion().getName() + (Strings.isNullOrEmpty(patch.getName()) ? "" : " (" + patch.getName() + ")");
			TimeablePanel<FarmingPatch> p = new TimeablePanel<>(patch, title, 1);

			/* Show labels to subdivide tabs into sections */
			if (patch.getImplementation() != lastImpl && !Strings.isNullOrEmpty(patch.getImplementation().getName()))
			{
				JLabel groupLabel = new JLabel(patch.getImplementation().getName());

				if (first)
				{
					first = false;
					groupLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
				}
				else
				{
					groupLabel.setBorder(new EmptyBorder(15, 0, 0, 0));
				}

				groupLabel.setFont(FontManager.getRunescapeSmallFont());

				add(groupLabel, c);
				c.gridy++;
				lastImpl = patch.getImplementation();
			}

			patchPanels.add(p);
			add(p, c);
			c.gridy++;

			/* This is a weird hack to remove the top border on the first tracker of every tab */
			if (first)
			{
				first = false;
				p.setBorder(null);
			}
		}

	}

	@Override
	public int getUpdateInterval()
	{
		return 50; // 10 seconds
	}

	@Override
	public void update()
	{
		long unixNow = Instant.now().getEpochSecond();

		for (TimeablePanel<FarmingPatch> panel : patchPanels)
		{
			FarmingPatch patch = panel.getTimeable();
			PatchPrediction prediction = farmingTracker.predictPatch(patch);

			if (prediction == null)
			{
				itemManager.getImage(Produce.WEEDS.getItemID()).addTo(panel.getIcon());
				panel.getIcon().setToolTipText("Unknown state");
				panel.getProgress().setMaximumValue(0);
				panel.getProgress().setValue(0);
				panel.getProgress().setVisible(false);
				panel.getEstimate().setText("Unknown");
				panel.getProgress().setBackground(null);
			}
			else
			{
				if (prediction.getProduce().getItemID() < 0)
				{
					panel.getIcon().setIcon(null);
					panel.getIcon().setToolTipText("Unknown state");
				}
				else
				{
					itemManager.getImage(prediction.getProduce().getItemID()).addTo(panel.getIcon());
					panel.getIcon().setToolTipText(prediction.getProduce().getName());
				}

				switch (prediction.getCropState())
				{
					case HARVESTABLE:
						panel.getEstimate().setText("Done");
						break;
					case GROWING:
						if (prediction.getDoneEstimate() < unixNow)
						{
							panel.getEstimate().setText("Done");
						}
						else
						{
							panel.getEstimate().setText("Done " + getFormattedEstimate(prediction.getDoneEstimate() - unixNow, config.estimateRelative()));
						}
						break;
					case DISEASED:
						panel.getEstimate().setText("Diseased");
						break;
					case DEAD:
						panel.getEstimate().setText("Dead");
						break;
				}

				/* Hide any fully grown weeds' progress bar. */
				if (prediction.getProduce() != Produce.WEEDS || prediction.getStage() < prediction.getStages() - 1)
				{
					panel.getProgress().setVisible(true);
					panel.getProgress().setForeground(prediction.getCropState().getColor().darker());
					panel.getProgress().setMaximumValue(prediction.getStages() - 1);
					panel.getProgress().setValue(prediction.getStage());
				}
				else
				{
					panel.getProgress().setVisible(false);
				}
			}
		}
	}
}
