import * as Types from '../types/__generated__/graphql';

import { gql } from '@apollo/client';
export type CatalogItemFieldsFragment = { __typename: 'CatalogItem', id: string, name: string, category: Types.CatalogItemCategory, priceCents: number, status: Types.CatalogItemStatus, description: string | null, createdAt: string, updatedAt: string };

export const CatalogItemFieldsFragmentDoc = gql`
    fragment CatalogItemFields on CatalogItem {
  id
  name
  category
  priceCents
  status
  description
  createdAt
  updatedAt
}
    `;