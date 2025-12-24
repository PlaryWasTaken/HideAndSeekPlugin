package org.plary.hide

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
object ItemBuilder {

    fun fromSection(section: ConfigurationSection): ItemStack {
        val material = Material.valueOf(section.getString("material")!!)
        val amount = section.getInt("amount", 1)

        val item = ItemStack(material, amount)

        item.editMeta { meta ->

            section.getString("name")?.let {
                meta.displayName(Component.text(it.replace("&", "§")))
            }

            meta.isUnbreakable = (section.getBoolean("unbreakable", false))

            section.getConfigurationSection("enchantments")?.let { enchants ->
                for (key in enchants.getKeys(false)) {
                    val enchant = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(key))
                        ?: continue
                    meta.addEnchant(enchant, enchants.getInt(key), true)
                }
            }

            section.getStringList("flags").forEach { flag ->
                meta.addItemFlags(ItemFlag.valueOf(flag))
            }
        }

        return item
    }
}
