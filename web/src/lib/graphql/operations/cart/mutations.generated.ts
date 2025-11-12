import * as Types from '../../types/__generated__/graphql';

import { gql } from '@apollo/client';
import * as ApolloReactCommon from '@apollo/client/react';
import * as ApolloReactHooks from '@apollo/client/react';
const defaultOptions = {} as const;
export type CheckoutMutationVariables = Types.Exact<{
  input: Types.CheckoutInput;
}>;


export type CheckoutMutation = { checkout: { __typename: 'CheckoutLog', id: string, userId: string | null, totalPriceCents: number, itemCount: number, status: Types.CheckoutStatus, createdAt: string, items: Array<{ __typename: 'CheckoutItem', catalogItemId: string, name: string, priceCents: number, quantity: number }> } };


export const CheckoutDocument = gql`
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

/**
 * __useCheckoutMutation__
 *
 * To run a mutation, you first call `useCheckoutMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useCheckoutMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [checkoutMutation, { data, loading, error }] = useCheckoutMutation({
 *   variables: {
 *      input: // value for 'input'
 *   },
 * });
 */
export function useCheckoutMutation(baseOptions?: ApolloReactHooks.MutationHookOptions<CheckoutMutation, CheckoutMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return ApolloReactHooks.useMutation<CheckoutMutation, CheckoutMutationVariables>(CheckoutDocument, options);
      }
export type CheckoutMutationHookResult = ReturnType<typeof useCheckoutMutation>;
export type CheckoutMutationResult = ApolloReactCommon.MutationResult<CheckoutMutation>;
