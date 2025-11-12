"use client";

import React, { useState } from "react";
import { Button, Chip, Skeleton } from "@/shared/components/ui/primitives";
import { TextField } from "@/shared/components/ui/forms/form";
import { Paper, Stack } from "@/shared/components/ui/layout";
import { Typography, Box, Alert, useTheme, IconButton, InputAdornment } from "@mui/material";
import { AddIcon, AddShoppingCartIcon, RemoveIcon, RefreshIcon } from "@/shared/ui/mui-imports";
import { useCatalog, CatalogItem } from "@/lib/hooks/catalog/useCatalog";
import { useCart } from "@/lib/hooks/cart/useCart";
import { useTranslation } from "@/shared/i18n";
import { catalogTranslations } from "../i18n";
import { useToast } from "@/shared/providers";
export interface CatalogSearchProps {
  onEnrollClick?: () => void;
}

export function CatalogSearch({ onEnrollClick }: CatalogSearchProps) {
  const { t } = useTranslation(catalogTranslations);
  const theme = useTheme();
  const {
    searchResults,
    searchQuery,
    setSearchQuery,
    searchLoading,
    searchError,
    getStatusColor,
    getCategoryLabel,
    refetch,
  } = useCatalog();
  const { addItem } = useCart();
  const toast = useToast();
  const [quantities, setQuantities] = useState<Record<string, number>>({});

  const hasResults = searchResults.length > 0;
  const showEmptyState = !searchLoading && searchQuery.trim().length === 0 && !hasResults;
  const showNoResults = !searchLoading && searchQuery.trim().length > 0 && !hasResults;

  const formatPrice = (priceCents: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(priceCents / 100);
  };

  const getQuantity = (itemId: string): number => {
    return quantities[itemId] || 1;
  };

  const setQuantity = (itemId: string, quantity: number) => {
    const validatedQuantity = Math.max(1, Math.min(9999, quantity));
    setQuantities((prev) => ({ ...prev, [itemId]: validatedQuantity }));
  };

  const handleAddToCart = (item: CatalogItem) => {
    const quantity = getQuantity(item.id);
    addItem({
      catalogItemId: item.id,
      name: item.name,
      priceCents: item.priceCents,
      quantity,
    });
    toast.success(t('toast.itemAdded', { name: item.name }));
  };

  return (
    <Stack gap={3}>
      <Paper sx={{ p: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            fullWidth
            placeholder={t('searchPlaceholder')}
            value={searchQuery}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
            startIcon="search"
            data-testid="catalog-search-input"
          />
          <Button
            variant="outlined"
            onClick={() => refetch()}
            disabled={searchLoading}
            data-testid="catalog-refresh-button"
            sx={{ 
              minHeight: '56px',
              minWidth: '56px',
              padding: '8px',
            }}
            aria-label="Refresh search"
          >
            <RefreshIcon />
          </Button>
          {onEnrollClick && (
            <Button
              variant="contained"
              onClick={onEnrollClick}
              startIcon={<AddIcon />}
              data-testid="create-new-item-button"
              sx={{ 
                minHeight: '56px',
              }}
            >
              {t('enrollNewItem')}
            </Button>
          )}
        </Box>
      </Paper>

      {searchError && (
        <Alert severity="error" data-testid="catalog-search-error">
          {t('searchError')}
        </Alert>
      )}

      {searchLoading && (
        <Stack gap={2}>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} variant="rectangular" height={100} />
          ))}
        </Stack>
      )}

      {showEmptyState && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="body1" color="text.secondary">
            {t('emptyStateMessage')}
          </Typography>
        </Paper>
      )}

      {showNoResults && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="body1" color="text.secondary" gutterBottom>
            {t('noResultsMessage', { query: searchQuery })}
          </Typography>
          {onEnrollClick && (
            <Button
              variant="contained"
              onClick={onEnrollClick}
              sx={{ mt: 2 }}
              data-testid="enroll-new-item-button"
            >
              {t('enrollNewItem')}
            </Button>
          )}
        </Paper>
      )}

      {hasResults && (
        <Box
          sx={{
            maxHeight: 'calc(100vh - 400px)',
            overflowY: 'auto',
            overflowX: 'hidden',
            pr: 1,
            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              backgroundColor: 'transparent',
            },
            '&::-webkit-scrollbar-thumb': {
              backgroundColor: theme.palette.divider,
              borderRadius: '4px',
              '&:hover': {
                backgroundColor: theme.palette.action.hover,
              },
            },
          }}
        >
          <Stack gap={2}>
            {searchResults.map((item: CatalogItem) => {
              const quantity = getQuantity(item.id);
              return (
                <Paper key={item.id} sx={{ p: 3 }}>
                  <Stack gap={2}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <Box>
                        <Typography variant="h6" component="h3">
                          {item.name}
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 1, mt: 1, flexWrap: 'wrap' }}>
                          <Chip
                            label={getCategoryLabel(item.category)}
                            size="small"
                            variant="outlined"
                          />
                          <Chip
                            label={item.status}
                            size="small"
                            color={getStatusColor(item.status)}
                          />
                        </Box>
                      </Box>
                      <Typography variant="h6" color="primary">
                        {formatPrice(item.priceCents)}
                      </Typography>
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {t('createdAt')}: {new Date(item.createdAt).toLocaleDateString()}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mt: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <IconButton
                          size="small"
                          onClick={() => setQuantity(item.id, quantity - 1)}
                          disabled={quantity <= 1}
                          aria-label="Decrease quantity"
                        >
                          <RemoveIcon />
                        </IconButton>
                        <TextField
                          type="number"
                          value={quantity}
                          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                            const value = parseInt(e.target.value, 10);
                            if (!isNaN(value) && value >= 1) {
                              setQuantity(item.id, value);
                            }
                          }}
                          inputProps={{
                            min: 1,
                            style: { textAlign: 'center', width: '60px' },
                          }}
                          sx={{ width: '100px' }}
                          size="small"
                        />
                        <IconButton
                          size="small"
                          onClick={() => setQuantity(item.id, quantity + 1)}
                          aria-label="Increase quantity"
                        >
                          <AddIcon />
                        </IconButton>
                      </Box>
                      <Button
                        variant="contained"
                        startIcon={<AddShoppingCartIcon />}
                        onClick={() => handleAddToCart(item)}
                        data-testid={`add-to-cart-${item.id}`}
                      >
                        {t('addToCart')}
                      </Button>
                    </Box>
                  </Stack>
                </Paper>
              );
            })}
          </Stack>
        </Box>
      )}
    </Stack>
  );
}

