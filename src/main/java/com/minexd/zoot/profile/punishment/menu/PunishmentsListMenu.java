package com.minexd.zoot.profile.punishment.menu;

import com.minexd.zoot.cache.RedisPlayerData;
import com.minexd.zoot.profile.Profile;
import com.minexd.zoot.profile.menu.ProfileMenuControlHeaderButton;
import com.minexd.zoot.profile.punishment.PunishmentType;
import com.minexd.zoot.profile.punishment.procedure.PunishmentProcedure;
import com.minexd.zoot.profile.punishment.procedure.PunishmentProcedureStage;
import com.minexd.zoot.profile.punishment.procedure.PunishmentProcedureType;
import com.minexd.zoot.util.menu.Button;
import com.minexd.zoot.profile.punishment.Punishment;
import com.minexd.zoot.util.ItemBuilder;
import com.minexd.zoot.util.TimeUtil;
import com.minexd.zoot.util.CC;
import com.minexd.zoot.util.menu.pagination.PageFilter;
import com.minexd.zoot.util.menu.pagination.FilterablePaginatedMenu;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class PunishmentsListMenu extends FilterablePaginatedMenu<Punishment> {

	private Profile profile;
	private RedisPlayerData redisPlayerData;

	@Override
	public String getPrePaginatedTitle(Player player) {
		return "&c" + profile.getName() + "'s Punishments (" + getCountOfFilteredPunishments() + ")";
	}

	@Override
	public Map<Integer, Button> getFilteredButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();

		obj:
		for (Punishment punishment : profile.getPunishments()) {
			for (PageFilter<Punishment> filter : getFilters()) {
				if (!filter.test(punishment)) {
					continue obj;
				}
			}

			buttons.put(buttons.size(), new PunishmentInfoButton(punishment));
		}

		return buttons;
	}

	@Override
	public Map<Integer, Button> getGlobalButtons(Player player) {
		Map<Integer, Button> buttons = super.getGlobalButtons(player);

		if (profile.getName() != null) {
			buttons.put(4, new ProfileMenuControlHeaderButton(profile, redisPlayerData));
		}

		return buttons;
	}

	@Override
	public List<PageFilter<Punishment>> generateFilters() {
		List<PageFilter<Punishment>> filters = new ArrayList<>();
		filters.add(new PageFilter<>("Type: Ban", (punishment -> punishment.getType() == PunishmentType.BAN)));
		filters.add(new PageFilter<>("Type: Mute", (punishment -> punishment.getType() == PunishmentType.MUTE)));
		filters.add(new PageFilter<>("Type: Warn", (punishment -> punishment.getType() == PunishmentType.WARN)));
		filters.add(new PageFilter<>("Type: Kick", (punishment -> punishment.getType() == PunishmentType.KICK)));
		filters.add(new PageFilter<>("Issued By Console", (punishment -> punishment.getAddedBy() == null)));
		filters.add(new PageFilter<>("Removed", Punishment::isRemoved));
		return filters;
	}

	private int getCountOfFilteredPunishments() {
		int i = 0;

		obj:
		for (Punishment punishment : profile.getPunishments()) {
			for (PageFilter<Punishment> filter : getFilters()) {
				if (!filter.test(punishment)) {
					continue obj;
				}
			}

			i++;
		}

		return i;
	}

	@AllArgsConstructor
	private class PunishmentInfoButton extends Button {

		private Punishment punishment;

		@Override
		public ItemStack getButtonItem(Player player) {
			int durability;

			if (punishment.isRemoved()) {
				durability = 5;
			} else if (punishment.hasExpired()) {
				durability = 4;
			} else {
				durability = 14;
			}

			String addedBy = "Console";

			if (punishment.getAddedBy() != null) {
				try {
					Profile addedByProfile = Profile.getByUuid(punishment.getAddedBy());
					addedBy = addedByProfile.getName();
				} catch (Exception e) {
					addedBy = "Could not fetch...";
				}
			}

			String removedBy = "Console";

			if (punishment.getRemovedBy() != null) {
				try {
					Profile removedByProfile = Profile.getByUuid(punishment.getRemovedBy());
					removedBy = removedByProfile.getName();
				} catch (Exception e) {
					removedBy = "Could not fetch...";
				}
			}

			List<String> lore = new ArrayList<>();
			lore.add(CC.MENU_BAR);
			lore.add("&3Type: &e" + punishment.getType().getReadable());
			lore.add("&3Duration: &e" + punishment.getDurationText());
			lore.add("&3Issued by: &e" + addedBy);
			lore.add("&3Reason: &e&o\"" + punishment.getAddedReason() + "\"");

			if (punishment.isRemoved()) {
				lore.add(CC.MENU_BAR);
				lore.add("&a&lPunishment Removed");
				lore.add("&a" + TimeUtil.dateToString(new Date(punishment.getRemovedAt()), "&7"));
				lore.add("&aRemoved by: &7" + removedBy);
				lore.add("&aReason: &7&o\"" + punishment.getRemovedReason() + "\"");
			} else {
				if (!punishment.hasExpired() && punishment.getType().isRemovable()) {
					lore.add(CC.MENU_BAR);
					lore.add("&aRight click to remove this punishment");
				}
			}

			lore.add(CC.MENU_BAR);

			return new ItemBuilder(Material.STAINED_GLASS_PANE)
					.durability(durability)
					.name("&3" + TimeUtil.dateToString(new Date(punishment.getAddedAt()), "&7"))
					.lore(lore)
					.build();
		}

		@Override
		public void clicked(Player player, ClickType clickType) {
			if (clickType == ClickType.RIGHT && !punishment.isRemoved() && !punishment.hasExpired() && punishment.getType().isRemovable()) {
				PunishmentProcedure procedure = new PunishmentProcedure(player, profile, PunishmentProcedureType.PARDON, PunishmentProcedureStage.REQUIRE_TEXT);
				procedure.setPunishment(punishment);

				player.sendMessage(CC.GREEN + "Enter a reason for removing this punishment.");
				player.closeInventory();
			}
		}
	}

}
