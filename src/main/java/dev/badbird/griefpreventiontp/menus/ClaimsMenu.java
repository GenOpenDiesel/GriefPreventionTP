package dev.badbird.griefpreventiontp.menus;

import dev.badbird.griefpreventiontp.GriefPreventionTP;
import dev.badbird.griefpreventiontp.api.ClaimInfo;
import dev.badbird.griefpreventiontp.api.IconWrapper;
import dev.badbird.griefpreventiontp.api.PlayerSortPreference;
import dev.badbird.griefpreventiontp.manager.MenuManager;
import dev.badbird.griefpreventiontp.manager.MessageManager;
import dev.badbird.griefpreventiontp.object.ComponentQuestionConversation;
import dev.badbird.griefpreventiontp.util.AdventureUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import net.badbird5907.blib.menu.buttons.Button;
import net.badbird5907.blib.menu.buttons.impl.CloseButton;
import net.badbird5907.blib.menu.buttons.impl.FilterButton;
import net.badbird5907.blib.menu.buttons.impl.NextPageButton;
import net.badbird5907.blib.menu.buttons.impl.PreviousPageButton;
import net.badbird5907.blib.menu.menu.PaginatedMenu;
import net.badbird5907.blib.util.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.*;
import java.util.function.BiPredicate;

public class ClaimsMenu extends PaginatedMenu {
    private final UUID uuid;
    private String searchTerm;
    private boolean privateClaims = true;
    
    // Sortowanie
    private SortType sortType = SortType.DEFAULT;
    private boolean sortAscending = true;
    private boolean sortPreferenceLoaded = false;

    public enum SortType {
        DEFAULT, // Data dodania (wg ID claima)
        NAME,    // Nazwa claima
        OWNER    // Nazwa właściciela
    }

    public ClaimsMenu(UUID uuid, String searchTerm) {
        this.uuid = uuid;
        this.searchTerm = searchTerm;
    }

    public ClaimsMenu(UUID uuid) {
        this.uuid = uuid;
        this.searchTerm = null;
    }


    @Override
    public String getPagesTitle(Player player) {
        return CC.translate(MenuManager.getString("claims", "title", "Claims"));
    }

    @Override
    public boolean showPageNumbersInTitle() {
        return false;
    }

    @Override
    public List<Button> getPaginatedButtons(Player player) {
        loadSortPreference(player);
        boolean hasPermission = true;
        if (GriefPreventionTP.getInstance().getConfig().getBoolean("teleport.permission.enabled", false)) {
            if (!player.hasPermission("gptp.teleport")) {
                hasPermission = false;
            }
        }
        List<Button> buttons = new ArrayList<>();
        Collection<ClaimInfo> claims = privateClaims ? GriefPreventionTP.getInstance().getClaimManager().getClaims(uuid) : GriefPreventionTP.getInstance().getClaimManager().getAllPublicClaims();
        
        // --- LOGIKA SORTOWANIA ---
        List<ClaimInfo> sortedClaims = new ArrayList<>(claims);
        sortedClaims.sort((c1, c2) -> {
            int result = 0;
            switch (sortType) {
                case NAME:
                    String name1 = c1.getName() != null ? c1.getName() : "";
                    String name2 = c2.getName() != null ? c2.getName() : "";
                    result = name1.compareToIgnoreCase(name2);
                    break;
                case OWNER:
                    String owner1 = c1.getOwnerName() != null ? c1.getOwnerName() : "";
                    String owner2 = c2.getOwnerName() != null ? c2.getOwnerName() : "";
                    result = owner1.compareToIgnoreCase(owner2);
                    break;
                case DEFAULT:
                default:
                    result = Long.compare(c1.getClaimID(), c2.getClaimID());
                    break;
            }
            return sortAscending ? result : -result;
        });

        for (ClaimInfo claim : sortedClaims) {
            if (searchTerm != null && !claim.getName().toLowerCase().contains(searchTerm.toLowerCase()) && !claim.getOwnerName().toLowerCase().contains(searchTerm.toLowerCase()))
                continue;
            buttons.add(new ClaimButton(claim, player, hasPermission));
        }
        return buttons;
    }

    private void loadSortPreference(Player player) {
        if (sortPreferenceLoaded) {
            return;
        }
        PlayerSortPreference preference = GriefPreventionTP.getInstance().getClaimManager().getPlayerSortPreference(player.getUniqueId());
        String storedSortType = preference.getSortType();
        try {
            sortType = storedSortType == null ? SortType.DEFAULT : SortType.valueOf(storedSortType);
        } catch (IllegalArgumentException ignored) {
            sortType = SortType.DEFAULT;
        }
        sortAscending = preference.isSortAscending();
        sortPreferenceLoaded = true;
    }

    @Override
    public List<Button> getEveryMenuSlots(Player player) {
        List<Button> buttons = new ArrayList<>();
        if (GriefPreventionTP.getInstance().getConfig().getBoolean("menu.enable-search", true)) {
            buttons.add(new SearchButton());
        }
        buttons.add(new SortButton()); // Dodanie przycisku sortowania do menu
        return buttons;
    }

    @Override
    public Button getFilterButton() {
        if (!plugin.getConfig().getBoolean("enable-public")) {
            return null;
        }
        return new FilterButton() {
            @Override
            public void clicked(Player player, ClickType type, int slot, InventoryClickEvent event) {
                privateClaims = !privateClaims;
                update(player);
            }

            @Override
            public ItemStack getItem(Player player) {
                String base = "filter." + (privateClaims ? "disabled" : "enabled");
                ItemStack itemStack = new ItemStack(Material.PAPER);
                Component name = AdventureUtil.getComponentFromConfig("claims", base + ".name", "<green>Viewing Public Claims: " + (!privateClaims ? "<green>Yes" : "<red>No"));
                List<Component> lore = AdventureUtil.getComponentListFromConfigDef("claims", base + ".lore", List.of("<gray>Click to toggle."));
                AdventureUtil.setItemDisplayName(itemStack, name);
                AdventureUtil.setItemLore(itemStack, lore);
                return itemStack;
            }

            @Override
            public int getSlot() {
                return 40;
            }
        };
    }

    @Override
    public Button getCloseButton() {
        return new CloseButton() {
            @Override
            public ItemStack getItem(Player player) {
                ItemStack itemStack = new ItemStack(Material.valueOf(plugin.getConfig().getString("menu.close-button.type", "BARRIER")));
                Component name = AdventureUtil.getComponentFromConfig("", "menu.close-button.name", "<red>Close");
                AdventureUtil.setItemDisplayName(itemStack, name);
                List<Component> lore = AdventureUtil.getComponentListFromConfig("", "menu.close-button.lore");
                if (!lore.isEmpty())
                    AdventureUtil.setItemLore(itemStack, lore);
                return itemStack;
            }

            @Override
            public int getSlot() {
                return plugin.getConfig().getBoolean("enable-public") ? 36 : 40;
            }
        };
    }

    @Override
    public Button getNextPageButton() {
        return new NextPageButton(this) {
            @Override
            public int getSlot() {
                return 41;
            }

            @Override
            public ItemStack getItem(Player player) {
                Material material = Material.valueOf(plugin.getConfig().getString("menu.next-page.type", "ARROW"));
                ItemStack item = new ItemStack(material);
                Component name = AdventureUtil.getComponentFromConfig("config", "menu.next-page.name", "<green>Next Page");
                List<Component> lore = AdventureUtil.getComponentListFromConfig("config", "menu.next-page.lore", List.of(
                        "<gray>Click to go to the next page."
                ));
                AdventureUtil.setItemDisplayName(item, name);
                AdventureUtil.setItemLore(item, lore);
                return item;
            }
        };
    }

    @Override
    public Button getPreviousPageButton() {
        return new PreviousPageButton(this) {
            @Override
            public int getSlot() {
                return 39;
            }

            @Override
            public ItemStack getItem(Player player) {
                Material material = Material.valueOf(plugin.getConfig().getString("menu.previous-page.type", "ARROW"));
                ItemStack item = new ItemStack(material);
                Component name = AdventureUtil.getComponentFromConfig("config", "menu.previous-page.name", "<green>Previous Page");
                List<Component> lore = AdventureUtil.getComponentListFromConfig("config", "menu.previous-page.lore", List.of(
                        "<gray>Click to go to the previous page."
                ));
                AdventureUtil.setItemDisplayName(item, name);
                AdventureUtil.setItemLore(item, lore);
                return item;
            }
        };
    }

    private class ClaimButton extends Button {
        private final ClaimInfo claimInfo;
        private Claim claim;
        private boolean canEdit;
        private final boolean hasPermission;

        public ClaimButton(ClaimInfo claimInfo, Player player, boolean hasPermission) {
            this.claimInfo = claimInfo;
            this.claim = claimInfo.getClaim();
            this.canEdit = player.hasPermission("gptp.staff") ||
                    GriefPreventionTP.getInstance().getPermissionsManager()
                            .hasClaimPermission(player, claim);
            this.hasPermission = hasPermission;
            claimInfo.checkValid();
        }

        @Override
        public ItemStack getItem(Player player) {
            IconWrapper setIcon = claimInfo.getIcon();
            ItemStack stack = setIcon != null ? setIcon.getItemStack() : new ItemStack(Material.PLAYER_HEAD);
            String name = AdventureUtil.getMiniMessageFromConfig("claims", "claim.name", "<green>{name}", "name", claimInfo.getName());
            int cost = GriefPreventionTP.getInstance().getTeleportManager().getTPCost(player, claimInfo.isPublic());
            boolean hasMoney = cost <= 0 || GriefPreventionTP.getInstance().getClaimManager().playerHasEnough(player, cost);
            List<String> lore1 =
                    new ArrayList<>(
                            AdventureUtil.getMiniMessageListFromConfigDef("claims", "claim.lore", new ArrayList<>(List.of(
                                            "<gray>Owner: {owner}",
                                            "<gray>ID: {id}",
                                            "<gray>{x}, {y}, {z}",
                                            "",
                                            "canTp:<gray>Click to teleport.",
                                            "noPerm:<red>No permission to teleport.",
                                            "canEdit:<gray>Right click to manage."
                                    )),
                                    "owner", claimInfo.getOwnerName(),
                                    "id", claimInfo.getClaimID(),
                                    "x", claimInfo.getSpawn().getX(),
                                    "y", claimInfo.getSpawn().getY(),
                                    "z", claimInfo.getSpawn().getZ(),
                                    "cost", cost
                            )
                    );
            boolean bedrock = Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
            List<Component> lore = processLore(lore1, canEdit, bedrock, hasPermission, hasMoney);
            AdventureUtil.setItemDisplayName(stack, MiniMessage.miniMessage().deserialize(name));
            AdventureUtil.setItemLore(stack, lore);

            UUID owner = claimInfo.getOwner();
            if (stack.getType() == Material.PLAYER_HEAD && setIcon == null) {
                SkullMeta skullMeta = (SkullMeta) stack.getItemMeta();
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                stack.setItemMeta(skullMeta);
            }
            return stack;
        }

        private static List<Component> processLore(List<String> lore1, boolean canEdit, boolean bedrock, boolean hasPermission, boolean hasMoney) {
            Map<String, BiPredicate<Boolean, Boolean>> conditions = Map.of(
                    "canEdit:bedrock:", (c, b) -> c && b,
                    "canEdit:java:", (c, b) -> c && !b,
                    "canEdit:", (c, b) -> c,
                    "bedrock:", (c, b) -> b,
                    "hasPerm:", (c, b) -> hasPermission,
                    "noPerm:", (c, b) -> !hasPermission,
                    "noMoney:", (c, b) -> !hasMoney,
                    "hasMoney:", (c, b) -> hasMoney,
                    "canTp:", (c,b) -> hasPermission && hasMoney
            );

            return lore1.stream().map(str -> {
                for (Map.Entry<String, BiPredicate<Boolean, Boolean>> entry : conditions.entrySet()) {
                    String prefix = entry.getKey();
                    BiPredicate<Boolean, Boolean> condition = entry.getValue();
                    if (str.startsWith(prefix)) {
                        if (condition.test(canEdit, bedrock)) {
                            return MiniMessage.miniMessage().deserialize(str.substring(prefix.length()));
                        }
                        return null;
                    }
                }
                return MiniMessage.miniMessage().deserialize(str);
            }).filter(Objects::nonNull).toList();
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public void onClick(Player player, int slot, ClickType clickType, InventoryClickEvent event) {
            if (clickType.isRightClick() && canEdit) {
                new ManageClaimMenu(claimInfo, ClaimsMenu.this).open(player);
                return;
            }
            if (!hasPermission) {
                MessageManager.sendMessage(player, "teleport.permission.no-permission-message");
                return;
            }
            if (claimInfo.getSpawn() == null) {
                MessageManager.sendMessage(player, "messages.no-spawn-set");
                return;
            }
            GriefPreventionTP.getInstance().getTeleportManager().teleport(player, claimInfo.getSpawn().getLocation(), claimInfo.isPublic());
        }
    }

    private class SearchButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            ItemStack item = new ItemStack(
                    Material.valueOf(MenuManager.getString("claims", "search.type", "OAK_SIGN"))
            );
            Component name = AdventureUtil.getComponentFromConfig("claims", "search.name", "<green>Search");
            List<Component> lore = AdventureUtil.getComponentListFromConfig("claims", "search.lore", List.of(
                    "<gray>Click to search claims!"
            ));
            AdventureUtil.setItemDisplayName(item, name);
            AdventureUtil.setItemLore(item, lore);
            return item;
        }

        @Override
        public int getSlot() {
            return 44;
        }

        @Override
        public void onClick(Player player, int slot, ClickType clickType, InventoryClickEvent event) {
            new ComponentQuestionConversation(MessageManager.getComponent("messages.search"), (a) -> {
                String answer = a.toLowerCase();
                List<String> cancelMessages = plugin.getConfig().getStringList("search.cancel-messages");
                if (cancelMessages.stream().anyMatch(s -> s.equalsIgnoreCase(answer))) {
                    searchTerm = null;
                    open(player);
                    return Prompt.END_OF_CONVERSATION;
                }

                searchTerm = answer;
                open(player);
                return Prompt.END_OF_CONVERSATION;
            }).start(player);
        }
    }

    // Nowy przycisk sortowania
    private class SortButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            ItemStack item = new ItemStack(Material.valueOf(MenuManager.getString("claims", "sort.type", "HOPPER")));
            Component name = AdventureUtil.getComponentFromConfig("claims", "sort.name", "<green>Sortowanie");

            String typeStr = "";
            switch (sortType) {
                case DEFAULT: typeStr = "Kolejność dodania"; break;
                case NAME: typeStr = "Nazwa bazy"; break;
                case OWNER: typeStr = "Właściciel"; break;
            }
            String orderStr = sortAscending ? "Rosnąco" : "Malejąco";

            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Aktualne sortowanie: <yellow>" + typeStr));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Kierunek: <yellow>" + orderStr));
            lore.add(MiniMessage.miniMessage().deserialize(""));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Kliknij <green>LPM</green>, aby zmienić tryb."));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Kliknij <green>PPM</green>, aby zmienić kierunek."));

            AdventureUtil.setItemDisplayName(item, name);
            AdventureUtil.setItemLore(item, lore);
            return item;
        }

        @Override
        public int getSlot() {
            return 43;
        }

        @Override
        public void onClick(Player player, int slot, ClickType clickType, InventoryClickEvent event) {
            if (clickType.isLeftClick()) {
                int nextOrdinal = (sortType.ordinal() + 1) % SortType.values().length;
                sortType = SortType.values()[nextOrdinal];
            } else if (clickType.isRightClick()) {
                sortAscending = !sortAscending;
            }
            GriefPreventionTP.getInstance().getClaimManager().setPlayerSortPreference(
                    player.getUniqueId(),
                    new PlayerSortPreference(sortType.name(), sortAscending)
            );
            update(player);
        }
    }
}
