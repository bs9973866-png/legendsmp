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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LegendSMP extends JavaPlugin implements Listener {

    // ====== НАСТРОЙКИ ======
    // Сколько убийств нужно ВСЕГО для каждого уровня (индекс = уровень).
    // Ур.1 — уже за 3 убийства, ур.10 — за 200.
    static final int[] NEED = {0, 3, 8, 15, 25, 40, 60, 85, 115, 150, 200};
    static final int MAX_LEVEL = NEED.length - 1;
    static final double DAMAGE_PER_LEVEL = 0.05;      // +5% урона за уровень (до +50%)
    static final double ARMOR_PER_PIECE = 0.06;       // -6% урона за часть брони
    static final double ARMOR_FULL_SET_BONUS = 0.06;  // доп. -6% за полный сет

    record Def(String id, String name, Material mat, NamedTextColor color,
               String desc, String[] shape, String ingredients) {}
    record Perk(int level, String key, String name, String desc) {}
    record Ench(int level, Enchantment ench, int power) {}

    static class Menu implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    final List<Def> defs = new ArrayList<>();
    final List<NamespacedKey> recipeKeys = new ArrayList<>();
    final Map<String, List<Perk>> perks = new HashMap<>();
    final Map<String, List<Ench>> enchs = new HashMap<>();
    final Map<String, Long> cooldowns = new HashMap<>();
    boolean busy = false;
    NamespacedKey ID, KILLS, BOOK;

    @Override
    public void onEnable() {
        ID = new NamespacedKey(this, "id");
        KILLS = new NamespacedKey(this, "kills");
        BOOK = new NamespacedKey(this, "book");

        // ================= МЕЧ ТЕНЕЙ =================
        ench("shadow_sword", 1, Enchantment.SHARPNESS, 1);
        ench("shadow_sword", 2, Enchantment.SHARPNESS, 2);
        ench("shadow_sword", 4, Enchantment.FIRE_ASPECT, 1);
        ench("shadow_sword", 5, Enchantment.SHARPNESS, 3);
        ench("shadow_sword", 6, Enchantment.LOOTING, 2);
        ench("shadow_sword", 8, Enchantment.SHARPNESS, 4);
        ench("shadow_sword", 9, Enchantment.FIRE_ASPECT, 2);
        ench("shadow_sword", 10, Enchantment.SHARPNESS, 5);
        ench("shadow_sword", 10, Enchantment.LOOTING, 3);
        perk("shadow_sword", 3, "dash", "Теневой шаг", "ПКМ: рывок вперёд (откат 8 с)");
        perk("shadow_sword", 5, "blind", "Слепота врага", "15% шанс ослепить цель на 2 с");
        perk("shadow_sword", 7, "lifesteal", "Кража жизни", "Лечит на 15% нанесённого урона");
        perk("shadow_sword", 10, "wither", "Печать смерти", "25% шанс наложить иссушение");

        // ================= ТОПОР БЕРСЕРКА =================
        ench("berserk_axe", 1, Enchantment.SHARPNESS, 1);
        ench("berserk_axe", 3, Enchantment.SHARPNESS, 2);
        ench("berserk_axe", 5, Enchantment.FIRE_ASPECT, 1);
        ench("berserk_axe", 6, Enchantment.SHARPNESS, 3);
        ench("berserk_axe", 7, Enchantment.LOOTING, 2);
        ench("berserk_axe", 9, Enchantment.SHARPNESS, 5);
        ench("berserk_axe", 10, Enchantment.EFFICIENCY, 5);
        perk("berserk_axe", 2, "slow", "Тяжёлый удар", "Замедляет цель на 2 с");
        perk("berserk_axe", 4, "rage", "Ярость", "При здоровье ≤50% урон +30%");
        perk("berserk_axe", 6, "warcry", "Боевой клич", "ПКМ: Сила II и Скорость на 10 с (откат 45 с)");
        perk("berserk_axe", 8, "lightning", "Гнев богов", "15% шанс удара молнии (6 урона)");

        // ================= ЛУК ОХОТНИКА =================
        ench("hunter_bow", 1, Enchantment.POWER, 1);
        ench("hunter_bow", 2, Enchantment.POWER, 2);
        ench("hunter_bow", 3, Enchantment.PUNCH, 1);
        ench("hunter_bow", 4, Enchantment.POWER, 3);
        ench("hunter_bow", 5, Enchantment.FLAME, 1);
        ench("hunter_bow", 7, Enchantment.POWER, 4);
        ench("hunter_bow", 8, Enchantment.PUNCH, 2);
        ench("hunter_bow", 9, Enchantment.POWER, 5);
        ench("hunter_bow", 10, Enchantment.INFINITY, 1);
        perk("hunter_bow", 3, "glow", "Метка охотника", "Цель светится 5 с");
        perk("hunter_bow", 5, "slow", "Опутывание", "Замедляет цель на 2 с");
        perk("hunter_bow", 8, "lightning", "Громовой выстрел", "15% шанс удара молнии (6 урона)");
        perk("hunter_bow", 10, "wither", "Яд смерти", "25% шанс наложить иссушение");

        // ---- Оружие ----
        // H = голова игрока. Формат ингредиентов: "символ=МАТЕРИАЛ,..."
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

    // ====== РЕГИСТРАЦИЯ ======
    void perk(String id, int level, String key, String name, String desc) {
        perks.computeIfAbsent(id, k -> new ArrayList<>()).add(new Perk(level, key, name, desc));
    }

    void ench(String id, int level, Enchantment e, int power) {
        enchs.computeIfAbsent(id, k -> new ArrayList<>()).add(new Ench(level, e, power));
    }

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

    // ====== УРОВНИ ======
    int lvl(int kills) {
        int l = 0;
        for (int i = 1; i < NEED.length; i++) if (kills >= NEED[i]) l = i;
        return l;
    }

    int killsOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return 0;
        return it.getItemMeta().getPersistentDataContainer().getOrDefault(KILLS, PersistentDataType.INTEGER, 0);
    }

    boolean has(Def d, int lv, String key) {
        for (Perk pk : perks.getOrDefault(d.id(), List.of()))
            if (pk.key().equals(key) && lv >= pk.level()) return true;
        return false;
    }

    boolean chance(double c) {
        return ThreadLocalRandom.current().nextDouble() < c;
    }

    String enchName(Enchantment e) {
        String k = e.getKey().getKey();
        return switch (k) {
            case "sharpness" -> "Острота";
            case "fire_aspect" -> "Заговор огня";
            case "looting" -> "Добыча";
            case "efficiency" -> "Эффективность";
            case "power" -> "Сила";
            case "punch" -> "Отбрасывание";
            case "flame" -> "Горение";
            case "infinity" -> "Бесконечность";
            default -> k;
        };
    }

    // ====== ПРЕДМЕТЫ ======
    static Component t(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    boolean isWeapon(Def d) { return !d.id().startsWith("guardian"); }

    List<Component> lore(Def d, int kills) {
        List<Component> l = new ArrayList<>();
        l.add(t(d.desc(), NamedTextColor.GRAY));
        l.add(Component.empty());
        if (isWeapon(d)) {
            int lv = lvl(kills);
            l.add(t("Уровень: " + lv + "/" + MAX_LEVEL
                    + " (+" + (int) Math.round(lv * DAMAGE_PER_LEVEL * 100) + "% урона)", NamedTextColor.GOLD));
            l.add(t("Убийств: " + kills, NamedTextColor.GRAY));
            l.add(t(lv < MAX_LEVEL
                    ? "До ур. " + (lv + 1) + ": ещё " + (NEED[lv + 1] - kills) + " уб."
                    : "Максимальный уровень!", NamedTextColor.DARK_GRAY));
            List<Perk> ps = perks.get(d.id());
            if (ps != null && !ps.isEmpty()) {
                l.add(Component.empty());
                l.add(t("Способности:", NamedTextColor.YELLOW));
                for (Perk pk : ps) {
                    if (lv >= pk.level()) {
                        l.add(t("✔ " + pk.name(), NamedTextColor.GREEN));
                        l.add(t("   " + pk.desc(), NamedTextColor.GRAY));
                    } else {
                        l.add(t("✖ " + pk.name() + " (ур. " + pk.level() + ")", NamedTextColor.DARK_GRAY));
                    }
                }
            }
        } else {
            l.add(t("Бонус растёт с каждой частью сета", NamedTextColor.GOLD));
        }
        return l;
    }

    /** Обновляет счётчик убийств, описание и зачарования предмета. */
    void refresh(ItemMeta m, Def d, int kills) {
        m.getPersistentDataContainer().set(KILLS, PersistentDataType.INTEGER, kills);
        m.lore(lore(d, kills));
        int lv = lvl(kills);
        for (Ench en : enchs.getOrDefault(d.id(), List.of()))
            if (lv >= en.level()) m.addEnchant(en.ench(), en.power(), true);
    }

    ItemStack build(Def d, int kills) {
        ItemStack it = new ItemStack(d.mat());
        ItemMeta m = it.getItemMeta();
        m.displayName(t(d.name(), d.color()));
        m.setUnbreakable(true);
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(ID, PersistentDataType.STRING, d.id());
        refresh(m, d, kills);
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

    // ====== ПКМ: книга и активные способности ======
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        Player p = e.getPlayer();

        if (it.getItemMeta().getPersistentDataContainer().has(BOOK, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            openMenu(p);
            return;
        }

        Def d = def(it);
        if (d == null || !isWeapon(d)) return;
        int lv = lvl(killsOf(it));

        if (has(d, lv, "dash")) {
            e.setCancelled(true);
            Vector dir = p.getLocation().getDirection();
            dir.setY(0);
            if (dir.lengthSquared() < 0.0001) return;
            if (!ready(p, "dash", 8)) return;
            dir.normalize().multiply(1.8).setY(0.3);
            p.setVelocity(dir);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        } else if (has(d, lv, "warcry")) {
            e.setCancelled(true);
            if (!ready(p, "warcry", 45)) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
            p.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
            p.sendActionBar(Component.text("Боевой клич!", NamedTextColor.RED));
        }
    }

    boolean ready(Player p, String key, int seconds) {
        String k = p.getUniqueId() + ":" + key;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(k);
        if (until != null && until > now) {
            p.sendActionBar(Component.text("Перезарядка: " + ((until - now) / 1000 + 1) + " с", NamedTextColor.RED));
            return false;
        }
        cooldowns.put(k, now + seconds * 1000L);
        return true;
    }

    // ====== ВХОД ИГРОКА ======
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

    // ====== УРОН И ПАССИВНЫЕ СПОСОБНОСТИ ======
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (busy) return;
        Entity dm = e.getDamager();
        if (dm instanceof Projectile pr && pr.getShooter() instanceof Player s) dm = s;
        if (!(dm instanceof Player p)) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        Def d = def(hand);
        if (d == null || !isWeapon(d)) return;
        int lv = lvl(killsOf(hand));

        double mult = 1 + lv * DAMAGE_PER_LEVEL;
        if (has(d, lv, "rage") && p.getHealth() <= p.getMaxHealth() * 0.5) mult += 0.3;
        double dmg = e.getDamage() * mult;
        e.setDamage(dmg);

        if (!(e.getEntity() instanceof LivingEntity target)) return;

        if (has(d, lv, "slow"))
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
        if (has(d, lv, "glow"))
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        if (has(d, lv, "blind") && chance(0.15))
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        if (has(d, lv, "wither") && chance(0.25))
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
        if (has(d, lv, "lifesteal"))
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + dmg * 0.15));
        if (has(d, lv, "lightning") && chance(0.15)) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (target.isDead()) return;
                busy = true;
                try {
                    target.damage(6.0, p);
                } finally {
                    busy = false;
                }
            }, 12L);
        }
    }

    // ====== ПРОКАЧКА ОТ УБИЙСТВ ======
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        Def d = def(hand);
        if (d == null || !isWeapon(d)) return;

        int old = killsOf(hand);
        int kills = old + 1;
        ItemMeta m = hand.getItemMeta();
        refresh(m, d, kills);
        hand.setItemMeta(m);
        p.getInventory().setItemInMainHand(hand);

        int lv = lvl(kills);
        if (lv > lvl(old)) {
            p.sendMessage(Component.text("★ " + d.name() + " достиг уровня " + lv + "!", NamedTextColor.GOLD));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            for (Perk pk : perks.getOrDefault(d.id(), List.of()))
                if (pk.level() == lv)
                    p.sendMessage(Component.text("  Новая способность: " + pk.name() + " — " + pk.desc(), NamedTextColor.GREEN));
            for (Ench en : enchs.getOrDefault(d.id(), List.of()))
                if (en.level() == lv)
                    p.sendMessage(Component.text("  Зачарование: " + enchName(en.ench()) + " " + en.power(), NamedTextColor.AQUA));
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
        if (a.length == 2 && a[0].equalsIgnoreCase("kills") && p.isOp()) {
            // /legend kills <число> — выставить счётчик убийств оружию в руке (для теста)
            ItemStack hand = p.getInventory().getItemInMainHand();
            Def d = def(hand);
            if (d != null && isWeapon(d)) {
                try {
                    int n = Integer.parseInt(a[1]);
                    ItemMeta m = hand.getItemMeta();
                    refresh(m, d, n);
                    hand.setItemMeta(m);
                    p.getInventory().setItemInMainHand(hand);
                } catch (NumberFormatException ex) {
                    p.sendMessage(Component.text("Нужно число", NamedTextColor.RED));
                }
            }
            return true;
        }
        openMenu(p);
        return true;
    }
}
