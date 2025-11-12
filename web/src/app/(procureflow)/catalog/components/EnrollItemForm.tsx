"use client";

import React from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Box } from '@mui/material';
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from '@/shared/components/ui/primitives';
import { TextField } from '@/shared/components/ui/forms';
import { TextField as MUITextField, MenuItem } from '@mui/material';
import { FormLayout, FormRow } from '@/shared/components/ui/forms/components';
import { useTranslation } from '@/shared/i18n';
import { catalogTranslations } from '../i18n';
import { CatalogItemFormData } from '@/lib/hooks/catalog/useCatalog';

export interface EnrollItemFormProps {
  /** Whether the dialog is open */
  open: boolean;
  /** Dialog close handler */
  onClose: () => void;
  /** Form submit handler */
  onSubmit: (data: CatalogItemFormData) => Promise<void>;
  /** Whether form is submitting */
  loading?: boolean;
}

export function EnrollItemForm({
  open,
  onClose,
  onSubmit,
  loading = false,
}: EnrollItemFormProps) {
  const { t } = useTranslation(catalogTranslations);

  const schema = z.object({
    name: z.string().min(1, t('validation.nameRequired')),
    category: z.enum(['MATERIAL', 'SERVICE'], {
      errorMap: () => ({ message: t('validation.categoryRequired') }),
    }),
    price: z.number().min(0.01, t('validation.priceRequired')),
    status: z.enum(['ACTIVE', 'PENDING_APPROVAL', 'INACTIVE'], {
      errorMap: () => ({ message: t('validation.statusRequired') }),
    }),
    description: z.string().optional(),
  });

  const {
    control,
    handleSubmit,
    reset,
    formState: { isSubmitting, errors },
  } = useForm<{
    name: string;
    category: 'MATERIAL' | 'SERVICE';
    price: number;
    status: 'ACTIVE' | 'PENDING_APPROVAL' | 'INACTIVE';
    description?: string;
  }>({
    resolver: zodResolver(schema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      name: "",
      category: 'MATERIAL',
      price: 0,
      status: 'PENDING_APPROVAL',
      description: "",
    },
  });

  // Reset form when dialog opens/closes
  React.useEffect(() => {
    if (open) {
      reset({
        name: "",
        category: 'MATERIAL',
        price: 0,
        status: 'PENDING_APPROVAL',
        description: "",
      });
    }
  }, [open, reset]);

  const handleFormSubmit = async (data: {
    name: string;
    category: 'MATERIAL' | 'SERVICE';
    price: number;
    status: 'ACTIVE' | 'PENDING_APPROVAL' | 'INACTIVE';
    description?: string;
  }) => {
    try {
      const trimmedDescription = data.description?.trim();
      const submitData: CatalogItemFormData = {
        name: data.name,
        category: data.category,
        priceCents: Math.round(data.price * 100), // Convert dollars to cents
        status: data.status,
        ...(trimmedDescription && { description: trimmedDescription }),
      };
      await onSubmit(submitData);
      onClose();
    } catch (error) {
      // Error handling is done in the parent component
      console.error('Error submitting form:', error);
    }
  };

  const categoryOptions = [
    { value: 'MATERIAL', label: t('category.material') },
    { value: 'SERVICE', label: t('category.service') },
  ];

  const statusOptions = [
    { value: 'ACTIVE', label: t('status.active') },
    { value: 'PENDING_APPROVAL', label: t('status.pendingApproval') },
    { value: 'INACTIVE', label: t('status.inactive') },
  ];

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      data-testid="enroll-item-dialog"
    >
      <form onSubmit={(e) => {
        e.preventDefault();
        void handleSubmit(handleFormSubmit)(e);
      }}>
        <DialogTitle>{t('enrollForm.title')}</DialogTitle>
        <DialogContent sx={{ py: 4 }}>
          <Box sx={{ 
            py: 2, 
            '& .MuiStack-root': { px: 0, maxWidth: '100%' },
            '& .MuiGrid-root': { width: '100%' }
          }}>
            <FormLayout maxWidth="100%">
                <FormRow>
                  <Controller
                    name="name"
                    control={control}
                    render={({ field, fieldState }) => (
                      <TextField
                        {...field}
                        label={t('enrollForm.nameLabel')}
                        error={!!fieldState.error}
                        helperText={fieldState.error?.message}
                        fullWidth
                        required
                        autoFocus
                        data-testid="enroll-form-name"
                      />
                    )}
                  />
                </FormRow>
                <FormRow>
                  <Controller
                    name="category"
                    control={control}
                    render={({ field, fieldState }) => (
                      <MUITextField
                        select
                        label={t('enrollForm.categoryLabel')}
                        error={!!fieldState.error}
                        helperText={fieldState.error?.message}
                        fullWidth
                        required
                        data-testid="enroll-form-category"
                        {...field}
                      >
                        {categoryOptions.map((option) => (
                          <MenuItem key={option.value} value={option.value}>
                            {option.label}
                          </MenuItem>
                        ))}
                      </MUITextField>
                    )}
                  />
                </FormRow>
                <FormRow>
                  <Controller
                    name="price"
                    control={control}
                    render={({ field, fieldState }) => (
                      <TextField
                        type="number"
                        label={t('enrollForm.priceLabel')}
                        error={!!fieldState.error}
                        helperText={fieldState.error?.message}
                        fullWidth
                        required
                        inputProps={{ step: 0.01, min: 0 }}
                        onChange={(e) => {
                          const value = parseFloat(e.target.value) || 0;
                          field.onChange(value);
                        }}
                        value={field.value || ''}
                        data-testid="enroll-form-price"
                      />
                    )}
                  />
                </FormRow>
                <FormRow>
                  <Controller
                    name="status"
                    control={control}
                    render={({ field, fieldState }) => (
                      <MUITextField
                        select
                        label={t('enrollForm.statusLabel')}
                        error={!!fieldState.error}
                        helperText={fieldState.error?.message}
                        fullWidth
                        required
                        data-testid="enroll-form-status"
                        {...field}
                      >
                        {statusOptions.map((option) => (
                          <MenuItem key={option.value} value={option.value}>
                            {option.label}
                          </MenuItem>
                        ))}
                      </MUITextField>
                    )}
                  />
                </FormRow>
                <FormRow>
                  <Controller
                    name="description"
                    control={control}
                    render={({ field, fieldState }) => (
                      <TextField
                        {...field}
                        label={t('enrollForm.descriptionLabel')}
                        error={!!fieldState.error}
                        helperText={fieldState.error?.message}
                        fullWidth
                        multiline
                        rows={4}
                        data-testid="enroll-form-description"
                      />
                    )}
                  />
                </FormRow>
              </FormLayout>
            </Box>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={onClose}
            disabled={isSubmitting || loading}
            data-testid="enroll-form-cancel"
          >
            {t('enrollForm.cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={isSubmitting || loading}
            data-testid="enroll-form-submit"
          >
            {loading || isSubmitting ? t('enrollForm.submitting') : t('enrollForm.submit')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

