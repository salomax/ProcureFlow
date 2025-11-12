"use client";

import React from "react";
import { 
  Box, 
  Typography, 
  Container, 
  Card, 
  CardContent
} from "@/shared/ui/mui-imports";
import ShoppingCartRoundedIcon from "@mui/icons-material/ShoppingCartRounded";
import Link from "next/link";
import { Logo } from "@/shared/ui/brand";

export default function WelcomePage() {
  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ textAlign: "center", mb: 6 }}>
        <Box sx={{ mb: 3, display: 'flex', justifyContent: 'center' }}>
          <Logo variant="blue" size="xlarge" />
        </Box>
        <Typography variant="h2" component="h1" gutterBottom>
          Welcome to ProcureFlow
        </Typography>
        <Typography variant="h5" color="text.secondary" sx={{ mx: "auto" }}>
          Streamline your procurement process with our catalog management system
        </Typography>
      </Box>

      <Box 
        sx={{ 
          display: "flex",
          justifyContent: "center",
          gap: 4 
        }}
      >
        <Card 
          component={Link} 
          href="/catalog" 
          sx={{ 
            textDecoration: "none", 
            maxWidth: 400,
            width: "100%",
            transition: "transform 0.2s ease-in-out",
            "&:hover": {
              transform: "translateY(-4px)",
              boxShadow: 4
            }
          }}
        >
          <CardContent sx={{ textAlign: "center", p: 4 }}>
            <ShoppingCartRoundedIcon sx={{ fontSize: 48, color: "primary.main", mb: 2 }} />
            <Typography variant="h5" component="h2" gutterBottom>
              Catalog
            </Typography>
            <Typography color="text.secondary">
              Search and enroll catalog items for your procurement needs
            </Typography>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}
