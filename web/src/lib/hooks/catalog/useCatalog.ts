"use client";

import { useState, useCallback, useEffect } from "react";
import {
  useSearchCatalogItemsQuery,
  SearchCatalogItemsDocument
} from '@/lib/graphql/operations/catalog/queries.generated';
import {
  useSaveCatalogItemMutation
} from '@/lib/graphql/operations/catalog/mutations.generated';
import { CatalogItemInput, CatalogItemCategory, CatalogItemStatus } from '@/lib/graphql/types/__generated__/graphql';

export type CatalogItem = {
  id: string;
  name: string;
  category: string;
  priceCents: number;
  status: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CatalogItemFormData = {
  name: string;
  category: 'MATERIAL' | 'SERVICE';
  priceCents: number;
  status: 'ACTIVE' | 'PENDING_APPROVAL' | 'INACTIVE';
  description?: string;
};

export type UseCatalogOptions = {
  initialSearchQuery?: string;
};

export type UseCatalogReturn = {
  // Data
  searchResults: CatalogItem[];
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  
  // Enrollment
  enrollItem: (data: CatalogItemFormData) => Promise<void>;
  
  // Loading states
  searchLoading: boolean;
  enrollLoading: boolean;
  
  // Error handling
  searchError: Error | undefined;
  enrollError: Error | undefined;
  
  // Utilities
  refetch: () => void;
  getStatusColor: (status: string) => "success" | "error" | "warning" | "default";
  getCategoryLabel: (category: string) => string;
};

/**
 * Custom hook for managing catalog item search and enrollment
 * 
 * This hook encapsulates all catalog-related business logic including:
 * - Search functionality
 * - Enrollment functionality
 * - Loading states and error handling
 * 
 * @param options - Configuration options for the hook
 * @returns Object containing all catalog management functionality
 */
export function useCatalog(options: UseCatalogOptions = {}): UseCatalogReturn {
  const [searchQuery, setSearchQuery] = useState(options.initialSearchQuery || "");
  
  // Debounce search query to avoid too many API calls
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState(searchQuery);
  
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // GraphQL hooks
  const { data: searchData, loading: searchLoading, error: searchError, refetch } = useSearchCatalogItemsQuery({
    variables: { query: debouncedSearchQuery || "" },
  });
  
  // Log errors for debugging
  useEffect(() => {
    if (searchError) {
      console.error('Catalog search error:', searchError);
      if ('graphQLErrors' in searchError) {
        console.error('GraphQL errors:', (searchError as any).graphQLErrors);
      }
      if ('networkError' in searchError) {
        console.error('Network error:', (searchError as any).networkError);
      }
    }
  }, [searchError]);
  
  const [saveCatalogItemMutation, { loading: enrollLoading, error: enrollError }] = useSaveCatalogItemMutation();

  // Derived data
  const searchResults = searchData?.searchCatalogItems || [];

  // Enrollment operation
  const enrollItem = useCallback(async (data: CatalogItemFormData) => {
    try {
      const input: CatalogItemInput = {
        name: data.name.trim(),
        category: data.category as CatalogItemCategory,
        priceCents: data.priceCents,
        status: data.status as CatalogItemStatus,
        description: data.description?.trim() || null,
      };

      const result = await saveCatalogItemMutation({
        variables: { input },
        refetchQueries: debouncedSearchQuery ? [{ query: SearchCatalogItemsDocument, variables: { query: debouncedSearchQuery } }] : [],
      });

      if (!result.data) {
        throw new Error('Failed to save catalog item');
      }

      // Refetch the latest search to show the newly enrolled item
      await refetch();
    } catch (err) {
      console.error('Error saving catalog item:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to save catalog item';
      throw new Error(errorMessage);
    }
  }, [saveCatalogItemMutation, debouncedSearchQuery, refetch]);

  // Utility functions
  const getStatusColor = useCallback((status: string): "success" | "error" | "warning" | "default" => {
    switch (status) {
      case 'ACTIVE': return "success";
      case 'INACTIVE': return "error";
      case 'PENDING_APPROVAL': return "warning";
      default: return "default";
    }
  }, []);

  const getCategoryLabel = useCallback((category: string): string => {
    switch (category) {
      case 'MATERIAL': return 'Material';
      case 'SERVICE': return 'Service';
      default: return category;
    }
  }, []);

  return {
    // Data
    searchResults,
    searchQuery,
    setSearchQuery,
    
    // Enrollment
    enrollItem,
    
    // Loading states
    searchLoading,
    enrollLoading,
    
    // Error handling
    searchError,
    enrollError,
    
    // Utilities
    refetch,
    getStatusColor,
    getCategoryLabel,
  };
}

