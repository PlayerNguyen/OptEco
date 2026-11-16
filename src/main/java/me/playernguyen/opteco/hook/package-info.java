/**
 * Optional integrations with other plugins.
 *
 * <p>PlaceholderAPI, MVdWPlaceholderAPI, BossShopPro, and ShopGUIPlus are
 * soft dependencies. Every hook is loaded reflectively or defensively and must
 * stay optional and null-safe when the other plugin is absent. Placeholder
 * value resolution lives in {@code me.playernguyen.opteco.placeholder}; hooks
 * only adapt provider requests to it.</p>
 */
package me.playernguyen.opteco.hook;
