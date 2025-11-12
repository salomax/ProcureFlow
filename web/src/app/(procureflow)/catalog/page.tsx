"use client";

import React, { useState } from "react";
import { PageLayout, PageHeader, Paper } from '@/shared/components/ui/layout';
import { CatalogSearch, EnrollItemForm } from './components';
import { useCatalog, CatalogItemFormData } from '@/lib/hooks/catalog/useCatalog';
import { useTranslation } from '@/shared/i18n';
import { catalogTranslations } from './i18n';
import { useToast } from '@/shared/providers';
import { extractErrorMessage } from '@/lib/hooks/customer/utils';

export default function CatalogPage() {
  const { t } = useTranslation(catalogTranslations);
  const toast = useToast();
  const [enrollDialogOpen, setEnrollDialogOpen] = useState(false);
  
  const {
    enrollItem,
    enrollLoading,
    enrollError,
  } = useCatalog();

  const handleEnrollClick = () => {
    setEnrollDialogOpen(true);
  };

  const handleEnrollClose = () => {
    setEnrollDialogOpen(false);
  };

  const handleEnrollSubmit = async (data: CatalogItemFormData) => {
    try {
      await enrollItem(data);
      toast.success(t('toast.itemEnrolled', { name: data.name }));
      setEnrollDialogOpen(false);
    } catch (err) {
      console.error('Error enrolling item:', err);
      const errorMessage = extractErrorMessage(
        err,
        t('toast.itemEnrollError')
      );
      toast.error(errorMessage);
    }
  };

  return (
    <PageLayout gap={2} maxWidth="xl">
      <PageHeader
        title={t('pageTitle')}
        subtitle={t('pageSubtitle')}
      />
      <CatalogSearch onEnrollClick={handleEnrollClick} />
      <EnrollItemForm
        open={enrollDialogOpen}
        onClose={handleEnrollClose}
        onSubmit={handleEnrollSubmit}
        loading={enrollLoading}
      />
    </PageLayout>
  );
}

