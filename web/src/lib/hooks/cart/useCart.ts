"use client";

import { useCartContext } from "@/shared/providers/CartProvider";
import { CartItem, CartItemInput } from "./types";

export type UseCartReturn = {
  // Cart state
  items: CartItem[];
  itemCount: number;
  totalPriceCents: number;
  isEmpty: boolean;

  // Cart operations
  addItem: (item: CartItemInput) => void;
  removeItem: (itemId: string) => void;
  updateQuantity: (itemId: string, quantity: number) => void;
  clearCart: () => void;
};

/**
 * Custom hook for managing shopping cart
 *
 * This hook provides access to cart state and operations.
 * It wraps the CartContext and provides a clean API for cart management.
 *
 * @returns Object containing cart state and operations
 */
export function useCart(): UseCartReturn {
  const context = useCartContext();

  return {
    items: context.items,
    itemCount: context.itemCount,
    totalPriceCents: context.totalPriceCents,
    isEmpty: context.isEmpty,
    addItem: context.addItem,
    removeItem: context.removeItem,
    updateQuantity: context.updateQuantity,
    clearCart: context.clearCart,
  };
}

