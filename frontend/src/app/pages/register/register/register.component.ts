// src/app/pages/register/register/register.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthenticationService } from '../../../services/authentication-service/authentication.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  userData = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  };

  message = '';
  error = '';

  constructor(
    private authService: AuthenticationService,
    private router: Router
  ) {}

  register() {
    this.error = '';
    this.message = '';

    this.authService.register(this.userData).subscribe({
      next: () => {
        this.message = 'Registration successful! Redirecting to login...';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.error = err.error?.message || 'Registration failed';
      },
    });
  }
}