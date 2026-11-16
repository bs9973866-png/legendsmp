package ru.legend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegendSMP extends JavaPlugin implements Listener {

    // ====== НАСТРОЙКИ ======
    static final int KILLS_PER_LEVEL = 10;   // убийств на 1 уровень
    static final int MAX_LEVEL = 5;          // максимальный уровень оружия
    static final double DAMAGE_PER_LEVEL = 0.08; // +8% урона за уровень
    static final double ARMOR_PER_PIECE = 0.06;  // -6% урона за часть брони
    static final double ARMOR_FULL_SET_BONUS = 0.06; // доп. -6% за полный сет

    record Def(String id, String name, Material mat, NamedTextColor color,
               String desc, String[] shape, String ingredients) {}

    static class Menu implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    final List<Def> defs = new ArrayList<>();
    final List<NamespacedKey> recipeKeys = new ArrayList<>();
    NamespacedKey ID, KILLS, BOOK;

    @Override
    public void onEnable() {
        ID = new NamespacedKey(this, "id");
        KILLS = new NamespacedKey(this, "kills");
        BOOK = new NamespacedKey(this, "book");

        // H = голова игрока. Формат ингредиентов: "символ=МАТЕРИАЛ,..."
        // ---- Оружие (прокачивается от убийств) ----
        reg("shadow_sword", "Меч Теней", Material.NETHERITE_SWORD, NamedTextColor.DARK_PURPLE,
                "Клинок, питающийся душами врагов.", "H=PLAYER_HEAD,D=DIAMOND,N=NETHERITE_INGOT,S=STICK",
                "HDH", " N ", " S ");
        reg("berserk_axe", "Топор Берсерка", Material.NETHERITE_AXE, NamedTextColor.RED,
                "Чем больше крови, тем он тяжелее.", "H=PLAYER_HEAD,D=DIAMOND,N=NETHERITE_INGOT,S=STICK",
                "DNH", "DS ", " S ");
        reg("hunter_bow", "Лук Охотника", Material.BOW, NamedTextColor.GREEN,
                "Не знает промаха.", "H=PLAYER_HEAD,N=NETHERITE_INGOT,S=STRING",
                " NS", "H S", " NS");

        // ---- Броня (бонус за части сета) ----
        reg("guardian_helmet", "Шлем Стража", Material.NETHERITE_HELMET, NamedTextColor.AQUA,
                "Часть сета Стража.", "H=PLAYER_HEAD,D=DIAMOND",
                "DHD", "D D");
        reg("guardian_chest", "Нагрудник Стража", Material.NETHERITE_CHESTPLATE, NamedTextColor.AQUA,
                "Часть сета Стража.", "H=PLAYER_HEAD,D=DIAMOND",
                "D D", "DHD", "DDD");
        reg("guardian_legs", "Поножи Стража", Material.NETHERITE_LEGGINGS, NamedTextColor.AQUA,
                "Часть сета Стража.", "H=PLAYER_HEAD,D=DIAMOND",
                "DHD", "D D", "D D");
        reg("guardian_boots", "Ботинки Стража", Material.NETHERITE_BOOTS, NamedTextColor.AQUA,
                "Часть сета Стража.", "H=PLAYER_HEAD,D=DIAMOND",
                "D D", "H H");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        recipeKeys.forEach(Bukkit::removeRecipe);
    }

    // ====== РЕГИСТРАЦИЯ КРАФТОВ ======
    void reg(String id, String name, Material mat, NamedTextColor color, String desc,
             String ingredients, String... shape) {
        Def d = new Def(id, name, mat, color, desc, shape, ingredients);
        defs.add(d);
        NamespacedKey key = new NamespacedKey(this, id);
        ShapedRecipe r = new ShapedRecipe(key, build(d, 0));
        r.shape(shape);
        parse(ingredients).forEach(r::setIngredient);
        Bukkit.addRecipe(r);
        recipeKeys.add(key);
    }

    Map<Character, Material> parse(String s) {
        Map<Character, Material> map = new LinkedHashMap<>();
        for (String part : s.split(",")) {
            String[] kv = part.split("=");
            map.put(kv[0].charAt(0), Material.valueOf(kv[1]));
        }
        return map;
    }

    // ====== ПРЕДМЕТЫ ======
    static Component t(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    boolean isWeapon(Def d) { return !d.id().startsWith("guardian"); }
    int lvl(int kills) { return Math.min(MAX_LEVEL, kills / KILLS_PER_LEVEL); }

    List<Component> lore(Def d, int kills) {
        List<Component> l = new ArrayList<>();
        l.add(t(d.desc(), NamedTextColor.GRAY));
        l.add(Component.empty());
        if (isWeapon(d)) {
            int lv = lvl(kills);
            l.add(t("Уровень: " + lv + "/" + MAX_LEVEL + " (+" + (int) Math.round(lv * DAMAGE_PER_LEVEL * 100) + "% урона)", NamedTextColor.GOLD));
            l.add(t("Убийств: " + kills, NamedTextColor.DARK_GRAY));
        } else {
            l.add(t("Бонус растёт с каждой частью сета", NamedTextColor.GOLD));
        }
        return l;
    }

    ItemStack build(Def d, int kills) {
        ItemStack it = new ItemStack(d.mat());
        ItemMeta m = it.getItemMeta();
        m.displayName(t(d.name(), d.color()));
        m.setUnbreakable(true);
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(ID, PersistentDataType.STRING, d.id());
        m.getPersistentDataContainer().set(KILLS, PersistentDataType.INTEGER, kills);
        m.lore(lore(d, kills));
        it.setItemMeta(m);
        return it;
    }

    Def def(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        String id = it.getItemMeta().getPersistentDataContainer().get(ID, PersistentDataType.STRING);
        if (id == null) return null;
        for (Def d : defs) if (d.id().equals(id)) return d;
        return null;
    }

    ItemStack book() {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta m = it.getItemMeta();
        m.displayName(t("Книга Легенд", NamedTextColor.GOLD));
        m.lore(List.of(t("ПКМ — открыть рецепты легендарных предметов", NamedTextColor.GRAY)));
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(BOOK, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    // ====== МЕНЮ РЕЦЕПТОВ ======
    void openMenu(Player p) {
        Menu h = new Menu();
        int size = ((defs.size() - 1) / 9 + 1) * 9;
        h.inv = Bukkit.createInventory(h, size, Component.text("Легендарные рецепты"));

        for (Def d : defs) {
            ItemStack it = build(d, 0);
            ItemMeta m = it.getItemMeta();
            List<Component> l = new ArrayList<>(m.lore());
            l.add(Component.empty());
            l.add(t("Рецепт (верстак):", NamedTextColor.YELLOW));

            Map<Character, Integer> counts = new LinkedHashMap<>();
            for (String row : d.shape())
                for (char c : row.toCharArray())
                    if (c != ' ') counts.merge(c, 1, Integer::sum);

            Map<Character, Material> ing = parse(d.ingredients());
            counts.forEach((c, n) -> {
                Material mat = ing.get(c);
                Component name = mat == Material.PLAYER_HEAD
                        ? Component.text("Голова игрока")
                        : Component.translatable(mat.translationKey());
                l.add(Component.text("• " + n + "× ").append(name)
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            });
            m.lore(l);
            it.setItemMeta(m);
            h.inv.addItem(it);
        }
        p.openInventory(h.inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack it = e.getItem();
        if (it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(BOOK, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            openMenu(e.getPlayer());
        }
    }

    // ====== ВХОД ИГРОКА: рецепты в книге + книга легенд ======
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.discoverRecipes(recipeKeys);
        if (!p.hasPlayedBefore()) p.getInventory().addItem(book());
    }

    // ====== ГОЛОВЫ ИГРОКОВ (только при убийстве игроком) ======
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        if (victim.getKiller() == null) return;
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(victim);
        head.setItemMeta(sm);
        e.getDrops().add(head);
    }

    // ====== УРОН ОРУЖИЯ ======
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        Entity dm = e.getDamager();
        if (dm instanceof Projectile pr && pr.getShooter() instanceof Player s) dm = s;
        if (!(dm instanceof Player p)) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        Def d = def(hand);
        if (d == null || !isWeapon(d)) return;

        int kills = hand.getItemMeta().getPersistentDataContainer().getOrDefault(KILLS, PersistentDataType.INTEGER, 0);
        e.setDamage(e.getDamage() * (1 + lvl(kills) * DAMAGE_PER_LEVEL));
    }

    // ====== ПРОКАЧКА ОТ УБИЙСТВ ======
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        Def d = def(hand);
        if (d == null || !isWeapon(d)) return;

        ItemMeta m = hand.getItemMeta();
        int kills = m.getPersistentDataContainer().getOrDefault(KILLS, PersistentDataType.INTEGER, 0) + 1;
        m.getPersistentDataContainer().set(KILLS, PersistentDataType.INTEGER, kills);
        m.lore(lore(d, kills));
        hand.setItemMeta(m);
        p.getInventory().setItemInMainHand(hand);

        if (kills % KILLS_PER_LEVEL == 0 && kills / KILLS_PER_LEVEL <= MAX_LEVEL) {
            p.sendMessage(Component.text(d.name() + " достиг уровня " + lvl(kills) + "!", NamedTextColor.GOLD));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    // ====== БОНУС БРОНИ ======
    @EventHandler
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        int pieces = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            Def d = def(a);
            if (d != null && d.id().startsWith("guardian")) pieces++;
        }
        if (pieces == 0) return;
        double reduce = pieces * ARMOR_PER_PIECE + (pieces == 4 ? ARMOR_FULL_SET_BONUS : 0);
        e.setDamage(e.getDamage() * (1 - reduce));
    }

    // ====== КОМАНДА ======
    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 2 && a[0].equalsIgnoreCase("give") && p.isOp()) {
            for (Def d : defs) if (d.id().equals(a[1])) p.getInventory().addItem(build(d, 0));
            return true;
        }
        openMenu(p);
        return true;
    }
}
