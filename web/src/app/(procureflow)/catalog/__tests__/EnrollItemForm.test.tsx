import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EnrollItemForm } from '../components/EnrollItemForm';
import { AppThemeProvider } from '@/styles/themes/AppThemeProvider';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/shared/i18n/config';
import { catalogTranslations } from '../i18n';

// Register catalog translations
i18n.addResourceBundle('en', 'catalog', catalogTranslations.en, true, true);
i18n.addResourceBundle('pt', 'catalog', catalogTranslations.pt, true, true);

const renderEnrollForm = (props = {}) => {
  return render(
    <I18nextProvider i18n={i18n}>
      <AppThemeProvider>
        <EnrollItemForm
          open={true}
          onClose={vi.fn()}
          onSubmit={vi.fn()}
          {...props}
        />
      </AppThemeProvider>
    </I18nextProvider>
  );
};

describe('EnrollItemForm', () => {
  const mockOnSubmit = vi.fn();
  const mockOnClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnSubmit.mockResolvedValue(undefined);
  });

  it('renders all form fields', () => {
    renderEnrollForm({ onSubmit: mockOnSubmit, onClose: mockOnClose });
    
    expect(screen.getByTestId('enroll-form-name')).toBeInTheDocument();
    expect(screen.getByTestId('enroll-form-category')).toBeInTheDocument();
    expect(screen.getByTestId('enroll-form-price')).toBeInTheDocument();
  });

  it('validates required fields', async () => {
    renderEnrollForm({ onSubmit: mockOnSubmit, onClose: mockOnClose });
    
    const form = screen.getByTestId('enroll-item-dialog').querySelector('form');
    expect(form).toBeInTheDocument();
    
    fireEvent.submit(form!);
    
    expect(await screen.findByText(/Item name is required/i)).toBeInTheDocument();
  });

  it('validates price is greater than 0', async () => {
    const user = userEvent.setup();
    renderEnrollForm({ onSubmit: mockOnSubmit, onClose: mockOnClose });
    
    const nameInput = screen.getByLabelText(/Item Name/i) as HTMLInputElement;
    const priceInput = screen.getByLabelText(/Price \(USD\)/i) as HTMLInputElement;
    
    await user.type(nameInput, 'Test Item');
    await user.clear(priceInput);
    await user.type(priceInput, '0');
    
    const form = screen.getByTestId('enroll-item-dialog').querySelector('form');
    expect(form).toBeInTheDocument();
    
    fireEvent.submit(form!);
    
    expect(await screen.findByText(/Price must be greater than 0/i)).toBeInTheDocument();
  });

  it('submits form with valid data', async () => {
    const user = userEvent.setup();
    renderEnrollForm({ onSubmit: mockOnSubmit, onClose: mockOnClose });
    
    const nameInput = screen.getByLabelText(/Item Name/i) as HTMLInputElement;
    const priceInput = screen.getByLabelText(/Price \(USD\)/i) as HTMLInputElement;
    const submitButton = screen.getByTestId('enroll-form-submit');
    
    await user.type(nameInput, 'Customized Team T-Shirt');
    await user.clear(priceInput);
    await user.type(priceInput, '25.00');
    await user.click(submitButton);
    
    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith({
        name: 'Customized Team T-Shirt',
        category: 'MATERIAL',
        priceCents: 2500, // Converted from dollars to cents
        status: 'PENDING_APPROVAL',
      });
    });
  });

  it('closes dialog when cancel is clicked', async () => {
    const user = userEvent.setup();
    renderEnrollForm({ onSubmit: mockOnSubmit, onClose: mockOnClose });
    
    const cancelButton = screen.getByTestId('enroll-form-cancel');
    await user.click(cancelButton);
    
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('disables submit button when loading', () => {
    renderEnrollForm({ 
      onSubmit: mockOnSubmit, 
      onClose: mockOnClose,
      loading: true 
    });
    
    const submitButton = screen.getByTestId('enroll-form-submit');
    expect(submitButton).toBeDisabled();
  });

  it('displays submitting text when loading', () => {
    renderEnrollForm({ 
      onSubmit: mockOnSubmit, 
      onClose: mockOnClose,
      loading: true 
    });
    
    expect(screen.getByText(/Saving.../i)).toBeInTheDocument();
  });
});

