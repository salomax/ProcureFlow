"use client";

import * as React from "react";
import { AppThemeProvider } from "@/styles/themes/AppThemeProvider";
import { AppQueryProvider } from "@/lib/api/AppQueryProvider";
import { GraphQLProvider } from "@/lib/graphql/GraphQLProvider";
import { ToastProvider, AuthProvider } from "@/shared/providers";
import { CartProvider } from "@/shared/providers/CartProvider";
import "@/shared/i18n/config";

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AppThemeProvider>
      <AppQueryProvider>
        <GraphQLProvider>
          <AuthProvider>
            <CartProvider>
              <ToastProvider>
                {children}
              </ToastProvider>
            </CartProvider>
          </AuthProvider>
        </GraphQLProvider>
      </AppQueryProvider>
    </AppThemeProvider>
  );
}
