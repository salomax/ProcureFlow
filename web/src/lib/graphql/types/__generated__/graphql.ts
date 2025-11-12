export type Maybe<T> = T | null;
export type InputMaybe<T> = T | null;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  join__FieldSet: { input: unknown; output: unknown; }
  link__Import: { input: unknown; output: unknown; }
};

export type BaseEntityInput = {
  name: Scalars['String']['input'];
};

export type CatalogItem = {
  __typename: 'CatalogItem';
  category: CatalogItemCategory;
  createdAt: Scalars['String']['output'];
  description: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  priceCents: Scalars['Int']['output'];
  status: CatalogItemStatus;
  updatedAt: Scalars['String']['output'];
};

export enum CatalogItemCategory {
  Material = 'MATERIAL',
  Service = 'SERVICE'
}

export type CatalogItemInput = {
  category: CatalogItemCategory;
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  priceCents: Scalars['Int']['input'];
  status: CatalogItemStatus;
};

export enum CatalogItemStatus {
  Active = 'ACTIVE',
  Inactive = 'INACTIVE',
  PendingApproval = 'PENDING_APPROVAL'
}

export type ChatInput = {
  message: Scalars['String']['input'];
  sessionId: Scalars['String']['input'];
};

export type ChatResponse = {
  __typename: 'ChatResponse';
  response: Scalars['String']['output'];
  sessionId: Scalars['String']['output'];
};

export type CheckoutInput = {
  itemCount: Scalars['Int']['input'];
  items: Array<CheckoutItemInput>;
  totalPriceCents: Scalars['Int']['input'];
};

export type CheckoutItem = {
  __typename: 'CheckoutItem';
  catalogItemId: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  priceCents: Scalars['Int']['output'];
  quantity: Scalars['Int']['output'];
};

export type CheckoutItemInput = {
  catalogItemId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  priceCents: Scalars['Int']['input'];
  quantity: Scalars['Int']['input'];
};

export type CheckoutLog = {
  __typename: 'CheckoutLog';
  createdAt: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  itemCount: Scalars['Int']['output'];
  items: Array<CheckoutItem>;
  status: CheckoutStatus;
  totalPriceCents: Scalars['Int']['output'];
  userId: Maybe<Scalars['ID']['output']>;
};

export enum CheckoutStatus {
  Completed = 'COMPLETED',
  Failed = 'FAILED'
}

export type Conversation = {
  __typename: 'Conversation';
  lastMessage: Maybe<Scalars['String']['output']>;
  messageCount: Scalars['Int']['output'];
  sessionId: Scalars['String']['output'];
};

export type Mutation = {
  __typename: 'Mutation';
  chat: ChatResponse;
  checkout: CheckoutLog;
  clearConversation: Scalars['Boolean']['output'];
  saveCatalogItem: CatalogItem;
  signIn: SignInPayload;
};


export type MutationChatArgs = {
  input: ChatInput;
};


export type MutationCheckoutArgs = {
  input: CheckoutInput;
};


export type MutationClearConversationArgs = {
  sessionId: Scalars['String']['input'];
};


export type MutationSaveCatalogItemArgs = {
  input: CatalogItemInput;
};


export type MutationSignInArgs = {
  input: SignInInput;
};

export type Query = {
  __typename: 'Query';
  catalogItem: Maybe<CatalogItem>;
  conversation: Maybe<Conversation>;
  currentUser: Maybe<User>;
  searchCatalogItems: Array<CatalogItem>;
};


export type QueryCatalogItemArgs = {
  id: Scalars['ID']['input'];
};


export type QueryConversationArgs = {
  sessionId: Scalars['String']['input'];
};


export type QuerySearchCatalogItemsArgs = {
  query: Scalars['String']['input'];
};

export type SignInInput = {
  email: Scalars['String']['input'];
  password: Scalars['String']['input'];
  rememberMe?: InputMaybe<Scalars['Boolean']['input']>;
};

export type SignInPayload = {
  __typename: 'SignInPayload';
  refreshToken: Maybe<Scalars['String']['output']>;
  token: Scalars['String']['output'];
  user: User;
};

export type User = {
  __typename: 'User';
  displayName: Maybe<Scalars['String']['output']>;
  email: Scalars['String']['output'];
  id: Scalars['ID']['output'];
};

export enum Join__Graph {
  App = 'APP',
  Assistant = 'ASSISTANT',
  Security = 'SECURITY'
}

export enum Link__Purpose {
  /** `EXECUTION` features provide metadata necessary for operation execution. */
  Execution = 'EXECUTION',
  /** `SECURITY` features provide metadata necessary to securely resolve fields. */
  Security = 'SECURITY'
}
