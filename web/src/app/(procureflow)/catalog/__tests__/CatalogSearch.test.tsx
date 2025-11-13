import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CatalogSearch } from '../components/CatalogSearch';
import { AppThemeProvider } from '@/styles/themes/AppThemeProvider';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/shared/i18n/config';
import { catalogTranslations } from '../i18n';
import { CartProvider, ToastProvider } from '@/shared/providers';

// Register catalog translations
i18n.addResourceBundle('en', 'catalog', catalogTranslations.en, true, true);
i18n.addResourceBundle('pt', 'catalog', catalogTranslations.pt, true, true);

// Mock the useCatalog hook
const mockSearchResults = [
  {
    id: '1',
    name: 'USB-C Cable - 1m',
    category: 'MATERIAL',
    priceCents: 1500,
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
];

const mockUseCatalog = {
  searchResults: [] as Array<{ id: string; name: string; category: string; priceCents: number; status: string; createdAt: string; updatedAt: string; }>,
  searchQuery: '',
  setSearchQuery: vi.fn(),
  searchLoading: false,
  searchError: undefined as Error | undefined,
  enrollItem: vi.fn(),
  enrollLoading: false,
  enrollError: undefined,
  refetch: vi.fn(),
  getStatusColor: vi.fn((status: string) => {
    if (status === 'ACTIVE') return 'success';
    if (status === 'PENDING_APPROVAL') return 'warning';
    return 'default';
  }),
  getCategoryLabel: vi.fn((category: string) => {
    if (category === 'MATERIAL') return 'Material';
    if (category === 'SERVICE') return 'Service';
    return category;
  }),
};

vi.mock('@/lib/hooks/catalog/useCatalog', () => ({
  useCatalog: vi.fn(() => mockUseCatalog),
}));

const renderCatalogSearch = (props = {}) => {
  return render(
    <I18nextProvider i18n={i18n}>
      <AppThemeProvider>
        <ToastProvider>
          <CartProvider>
            <CatalogSearch {...props} />
          </CartProvider>
        </ToastProvider>
      </AppThemeProvider>
    </I18nextProvider>
  );
};

describe('CatalogSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseCatalog.searchResults = [];
    mockUseCatalog.searchQuery = '';
    mockUseCatalog.searchLoading = false;
    mockUseCatalog.searchError = undefined;
  });

  it('renders search input', () => {
    renderCatalogSearch();
    expect(screen.getByTestId('catalog-search-input')).toBeInTheDocument();
  });

  it('displays empty state message when no search query', () => {
    renderCatalogSearch();
    expect(screen.getByText(/Enter a search term/i)).toBeInTheDocument();
  });

  it('displays search results when items are found', () => {
    mockUseCatalog.searchResults = mockSearchResults;
    renderCatalogSearch();
    
    expect(screen.getByText('USB-C Cable - 1m')).toBeInTheDocument();
    expect(screen.getByText('Material')).toBeInTheDocument();
  });

  it('displays no results message and enroll button when search returns empty', () => {
    mockUseCatalog.searchQuery = 'Team T-Shirt';
    mockUseCatalog.searchResults = [];
    
    const onEnrollClick = vi.fn();
    renderCatalogSearch({ onEnrollClick });
    
    expect(screen.getByText(/No items found/i)).toBeInTheDocument();
    expect(screen.getByTestId('enroll-new-item-button')).toBeInTheDocument();
  });

  it('calls onEnrollClick when enroll button is clicked', async () => {
    const user = userEvent.setup();
    mockUseCatalog.searchQuery = 'Team T-Shirt';
    mockUseCatalog.searchResults = [];
    
    const onEnrollClick = vi.fn();
    renderCatalogSearch({ onEnrollClick });
    
    const enrollButton = screen.getByTestId('enroll-new-item-button');
    await user.click(enrollButton);
    
    expect(onEnrollClick).toHaveBeenCalledTimes(1);
  });

  it('displays loading skeleton when searching', () => {
    mockUseCatalog.searchLoading = true;
    renderCatalogSearch();
    
    // Should show loading state (skeletons)
    expect(screen.queryByText(/Enter a search term/i)).not.toBeInTheDocument();
  });

  it('displays error message when search fails', () => {
    mockUseCatalog.searchError = { message: 'Search failed' } as Error;
    renderCatalogSearch();
    
    expect(screen.getByTestId('catalog-search-error')).toBeInTheDocument();
  });
});

