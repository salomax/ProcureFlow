"use client";

import * as React from "react";
import { Drawer } from "@/shared/components/ui/layout";
import { Button } from "@/shared/components/ui/primitives";
import { Stack, Paper, Typography, Box, Divider, IconButton } from "@mui/material";
import { useCart } from "@/lib/hooks/cart/useCart";
import { useTranslation } from "@/shared/i18n";
import { cartTranslations } from "../i18n";
import { useToast } from "@/shared/providers";
import { DeleteIcon, AddIcon, RemoveIcon } from "@/shared/ui/mui-imports";
import { TextField } from "@/shared/components/ui/forms/form";
import { CheckoutConfirmation } from "./CheckoutConfirmation";
import { useCheckoutMutation } from "@/lib/graphql/operations/cart/mutations.generated";

type ViewState = "cart" | "checkout" | "confirmation";

export interface CartDrawerProps {
  open: boolean;
  onClose: () => void;
}

/**
 * Format price in cents to currency string
 */
const formatPrice = (priceCents: number): string => {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(priceCents / 100);
};

export const CartDrawer: React.FC<CartDrawerProps> = ({ open, onClose }) => {
  const { t } = useTranslation(cartTranslations);
  const toast = useToast();
  const { items, itemCount, totalPriceCents, isEmpty, removeItem, updateQuantity, clearCart } = useCart();
  const [viewState, setViewState] = React.useState<ViewState>("cart");
  const [checkoutMutation, { loading: checkoutLoading }] = useCheckoutMutation();

  // Reset to cart view when drawer closes
  React.useEffect(() => {
    if (!open) {
      setViewState("cart");
    }
  }, [open]);

  const handleProceedToCheckout = () => {
    if (isEmpty) {
      toast.error(t("errors.emptyCart"));
      return;
    }
    setViewState("checkout");
  };

  const handleBackToCart = () => {
    setViewState("cart");
  };

  const handleConfirmCheckout = async () => {
    if (isEmpty) {
      toast.error(t("errors.emptyCart"));
      return;
    }
    
    try {
      // Call GraphQL mutation
      const result = await checkoutMutation({
        variables: {
          input: {
            items: items.map(item => ({
              catalogItemId: item.catalogItemId,
              name: item.name,
              priceCents: item.priceCents,
              quantity: item.quantity,
            })),
            totalPriceCents,
            itemCount,
          },
        },
      });
      
      if (result.data?.checkout) {
        toast.success(t("toast.checkoutSuccess"));
        setViewState("confirmation");
      } else {
        throw new Error("Checkout failed");
      }
    } catch (error) {
      console.error("Checkout error:", error);
      
      // Extract error message from Apollo error
      let errorMessage = t("errors.checkoutFailed");
      if (error instanceof Error) {
        // Check if it's an Apollo error with GraphQL errors
        const apolloError = error as any;
        if (apolloError.graphQLErrors && apolloError.graphQLErrors.length > 0) {
          errorMessage = apolloError.graphQLErrors[0].message || errorMessage;
        } else if (apolloError.networkError) {
          errorMessage = apolloError.networkError.message || errorMessage;
        } else if (error.message) {
          errorMessage = error.message;
        }
      }
      
      toast.error(errorMessage);
    }
  };

  const handleQuantityChange = (itemId: string, newQuantity: number) => {
    if (newQuantity < 1) {
      removeItem(itemId);
      toast.success(t("toast.itemRemoved", { name: items.find((i) => i.id === itemId)?.name || "" }));
    } else {
      updateQuantity(itemId, newQuantity);
    }
  };

  const handleRemoveItem = (itemId: string) => {
    const item = items.find((i) => i.id === itemId);
    removeItem(itemId);
    if (item) {
      toast.success(t("toast.itemRemoved", { name: item.name }));
    }
  };

  const handleClearCart = () => {
    clearCart();
    toast.success(t("toast.cartCleared"));
  };

  const handleCheckoutComplete = () => {
    clearCart();
    setViewState("cart");
    onClose();
  };

  const getTitle = () => {
    switch (viewState) {
      case "checkout":
        return t("drawer.checkoutTitle");
      case "confirmation":
        return t("drawer.confirmationTitle");
      default:
        return t("drawer.cartTitle");
    }
  };

  // Render empty cart state
  const renderEmptyCart = () => (
    <Box sx={{ p: 4, textAlign: "center" }}>
      <Typography variant="h6" color="text.secondary" gutterBottom>
        {t("emptyCart.message")}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {t("emptyCart.submessage")}
      </Typography>
    </Box>
  );

  // Render cart item
  const renderCartItem = (item: typeof items[0], showQuantityControls = true) => {
    const subtotal = item.priceCents * item.quantity;

    return (
      <Paper key={item.id} sx={{ p: 2 }}>
        <Stack gap={2}>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" component="h3">
                {item.name}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {formatPrice(item.priceCents)} {t("item.price")}
              </Typography>
            </Box>
            <IconButton
              size="small"
              onClick={() => handleRemoveItem(item.id)}
              aria-label={t("item.remove")}
              color="error"
            >
              <DeleteIcon />
            </IconButton>
          </Box>

          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            {showQuantityControls ? (
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <IconButton
                  size="small"
                  onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                  aria-label="Decrease quantity"
                  disabled={item.quantity <= 1}
                >
                  <RemoveIcon />
                </IconButton>
                <TextField
                  type="number"
                  value={item.quantity}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    const value = parseInt(e.target.value, 10);
                    if (!isNaN(value) && value >= 1) {
                      handleQuantityChange(item.id, value);
                    }
                  }}
                  inputProps={{
                    min: 1,
                    style: { textAlign: "center", width: "60px" },
                  }}
                  sx={{ width: "80px" }}
                />
                <IconButton
                  size="small"
                  onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                  aria-label="Increase quantity"
                >
                  <AddIcon />
                </IconButton>
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary">
                {t("item.quantity")}: {item.quantity}
              </Typography>
            )}
            <Typography variant="h6" color="primary">
              {formatPrice(subtotal)}
            </Typography>
          </Box>
        </Stack>
      </Paper>
    );
  };

  // Render cart summary
  const renderSummary = () => (
    <Paper sx={{ p: 2, bgcolor: "background.default" }}>
      <Stack gap={1}>
        <Box sx={{ display: "flex", justifyContent: "space-between" }}>
          <Typography variant="body2" color="text.secondary">
            {t("summary.itemCount", { count: itemCount })}
          </Typography>
          <Typography variant="body2">{formatPrice(totalPriceCents)}</Typography>
        </Box>
        <Divider />
        <Box sx={{ display: "flex", justifyContent: "space-between" }}>
          <Typography variant="h6">{t("summary.total")}</Typography>
          <Typography variant="h6" color="primary">
            {formatPrice(totalPriceCents)}
          </Typography>
        </Box>
      </Stack>
    </Paper>
  );

  // Render cart view
  const renderCartView = () => (
    <Stack gap={2} sx={{ p: 2, height: "100%", display: "flex", flexDirection: "column" }}>
      {isEmpty ? (
        renderEmptyCart()
      ) : (
        <>
          <Box sx={{ flex: 1, overflow: "auto" }}>
            <Stack gap={2}>
              {items.map((item) => renderCartItem(item, true))}
            </Stack>
          </Box>
          {renderSummary()}
          <Stack direction="row" gap={1}>
            <Button variant="outlined" onClick={handleClearCart} fullWidth>
              {t("actions.clearCart")}
            </Button>
            <Button variant="contained" onClick={handleProceedToCheckout} fullWidth>
              {t("actions.proceedToCheckout")}
            </Button>
          </Stack>
        </>
      )}
    </Stack>
  );

  // Render checkout view
  const renderCheckoutView = () => (
    <Stack gap={2} sx={{ p: 2, height: "100%", display: "flex", flexDirection: "column" }}>
      <Typography variant="h6" gutterBottom>
        {t("checkout.reviewItems")}
      </Typography>
      <Box sx={{ flex: 1, overflow: "auto" }}>
        <Stack gap={2}>
          {items.map((item) => renderCartItem(item, true))}
        </Stack>
      </Box>
      <Paper sx={{ p: 2, bgcolor: "background.default" }}>
        <Typography variant="h6" gutterBottom>
          {t("checkout.orderSummary")}
        </Typography>
        {renderSummary()}
      </Paper>
      <Stack direction="row" gap={1}>
        <Button variant="outlined" onClick={handleBackToCart} fullWidth disabled={checkoutLoading}>
          {t("actions.backToCart")}
        </Button>
        <Button variant="contained" onClick={handleConfirmCheckout} fullWidth loading={checkoutLoading}>
          {t("actions.confirmCheckout")}
        </Button>
      </Stack>
    </Stack>
  );

  // Render confirmation view
  const renderConfirmationView = () => (
    <CheckoutConfirmation
      items={items}
      totalPriceCents={totalPriceCents}
      itemCount={itemCount}
      onContinueShopping={handleCheckoutComplete}
    />
  );

  const renderContent = () => {
    switch (viewState) {
      case "checkout":
        return renderCheckoutView();
      case "confirmation":
        return renderConfirmationView();
      default:
        return renderCartView();
    }
  };

  return (
    <Drawer
      open={open}
      onClose={onClose}
      anchor="right"
      title={getTitle()}
      showCloseButton={true}
      sx={{
        '& .MuiDrawer-paper': {
          borderRadius: 0,
        },
      }}
    >
      {renderContent()}
    </Drawer>
  );
};

