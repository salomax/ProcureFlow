/**
 * Cart-related TypeScript types
 */

/**
 * Represents an item in the shopping cart
 */
export type CartItem = {
  /** Unique identifier for the cart item */
  id: string;
  /** Reference to the catalog item ID */
  catalogItemId: string;
  /** Display name of the item */
  name: string;
  /** Price in cents */
  priceCents: number;
  /** Quantity of this item in the cart */
  quantity: number;
};

/**
 * Represents the entire shopping cart
 */
export type Cart = {
  /** Array of items in the cart */
  items: CartItem[];
  /** Total number of items (sum of all quantities) */
  itemCount: number;
  /** Total price in cents (sum of all item subtotals) */
  totalPriceCents: number;
  /** Whether the cart is empty */
  isEmpty: boolean;
};

/**
 * Input type for adding items to cart
 */
export type CartItemInput = {
  /** Reference to the catalog item ID */
  catalogItemId: string;
  /** Display name of the item */
  name: string;
  /** Price in cents */
  priceCents: number;
  /** Quantity to add (default: 1) */
  quantity?: number;
};

