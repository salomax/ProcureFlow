import { gql } from '@apollo/client';

// CatalogItem fields fragment
export const CATALOG_ITEM_FIELDS = gql`
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

