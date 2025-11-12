"use client";

import * as React from "react";
import { Stack, Paper, Typography, Box, Divider } from "@mui/material";
import { Button } from "@/shared/components/ui/primitives";
import { useTranslation } from "@/shared/i18n";
import { cartTranslations } from "../i18n";
import { CartItem } from "@/lib/hooks/cart/types";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";

export interface CheckoutConfirmationProps {
  items: CartItem[];
  totalPriceCents: number;
  itemCount: number;
  onContinueShopping: () => void;
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

/**
 * Checkout confirmation component
 * 
 * Displays a success message and order summary after checkout completion
 */
export const CheckoutConfirmation: React.FC<CheckoutConfirmationProps> = ({
  items,
  totalPriceCents,
  itemCount,
  onContinueShopping,
}) => {
  const { t } = useTranslation(cartTranslations);
  const orderId = React.useMemo(() => {
    // Generate a simple order ID (in real app, this would come from server)
    return `ORD-${Date.now().toString(36).toUpperCase()}`;
  }, []);
  const timestamp = React.useMemo(() => {
    return new Date().toLocaleString();
  }, []);

  return (
    <Stack gap={3} sx={{ p: 3, textAlign: "center" }}>
      <Box>
        <CheckCircleIcon sx={{ fontSize: 64, color: "success.main", mb: 2 }} />
        <Typography variant="h5" gutterBottom color="success.main">
          {t("confirmation.success")}
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {t("confirmation.message")}
        </Typography>
      </Box>

      <Paper sx={{ p: 2, bgcolor: "background.default" }}>
        <Stack gap={1}>
          <Box sx={{ display: "flex", justifyContent: "space-between" }}>
            <Typography variant="body2" color="text.secondary">
              {t("confirmation.orderId")}:
            </Typography>
            <Typography variant="body2" fontWeight="medium">
              {orderId}
            </Typography>
          </Box>
          <Box sx={{ display: "flex", justifyContent: "space-between" }}>
            <Typography variant="body2" color="text.secondary">
              {t("confirmation.timestamp")}:
            </Typography>
            <Typography variant="body2" fontWeight="medium">
              {timestamp}
            </Typography>
          </Box>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          {t("checkout.orderSummary")}
        </Typography>
        <Stack gap={1} sx={{ mb: 2 }}>
          {items.map((item) => (
            <Box key={item.id} sx={{ display: "flex", justifyContent: "space-between" }}>
              <Typography variant="body2">
                {item.name} x{item.quantity}
              </Typography>
              <Typography variant="body2" fontWeight="medium">
                {formatPrice(item.priceCents * item.quantity)}
              </Typography>
            </Box>
          ))}
        </Stack>
        <Divider sx={{ my: 1 }} />
        <Box sx={{ display: "flex", justifyContent: "space-between" }}>
          <Typography variant="h6">{t("summary.total")}</Typography>
          <Typography variant="h6" color="primary">
            {formatPrice(totalPriceCents)}
          </Typography>
        </Box>
      </Paper>

      <Button variant="contained" onClick={onContinueShopping} fullWidth size="large">
        {t("actions.continueShopping")}
      </Button>
    </Stack>
  );
};

