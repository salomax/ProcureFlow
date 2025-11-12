import { gql } from '@apollo/client';

export const CHECKOUT = gql`
  mutation Checkout($input: CheckoutInput!) {
    checkout(input: $input) {
      id
      userId
      items {
        catalogItemId
        name
        priceCents
        quantity
      }
      totalPriceCents
      itemCount
      status
      createdAt
    }
  }
`;

