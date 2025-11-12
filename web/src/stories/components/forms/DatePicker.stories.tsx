import type { Meta, StoryObj } from '@storybook/react';
import { DatePickerField } from '@/shared/components/ui/forms/form/DatePickers';
import { Stack, Box } from '@mui/material';
import { FormProvider, useForm } from 'react-hook-form';
import { useState } from 'react';

const meta: Meta<typeof DatePickerField> = {
  title: 'Components/Forms/DatePicker',
  component: DatePickerField,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'Date picker component for selecting dates and times.'
      }
    }
  },
  argTypes: {
    label: {
      control: { type: 'text' },
      description: 'Label for the date picker'
    },
    helperText: {
      control: { type: 'text' },
      description: 'Helper text below the field'
    }
  },
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => {
    const methods = useForm();
    return (
      <FormProvider {...methods}>
        <DatePickerField name="date" label="Select Date" />
      </FormProvider>
    );
  },
};

export const WithHelperText: Story = {
  render: () => {
    const methods = useForm();
    return (
      <FormProvider {...methods}>
        <DatePickerField name="date" label="Select Date" helperText="Choose a date" />
      </FormProvider>
    );
  },
};

export const WithError: Story = {
  render: () => {
    const methods = useForm({
      defaultValues: { date: null },
      mode: 'onChange'
    });
    return (
      <FormProvider {...methods}>
        <DatePickerField name="date" label="Select Date" helperText="This field has an error" />
      </FormProvider>
    );
  },
};
