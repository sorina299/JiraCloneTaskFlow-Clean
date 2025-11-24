import { Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthenticationService } from '../services/authentication-service/authentication.service';

export const NoAuthGuard: CanActivateFn = () => {
  const auth = inject(AuthenticationService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) return true;

  router.navigate(['/projects']);
  return false;
};