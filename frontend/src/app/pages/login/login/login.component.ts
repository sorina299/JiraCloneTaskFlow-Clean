// src/app/pages/login/login/login.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthenticationService } from '../../../services/authentication-service/authentication.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  credentials = {
    username: '',
    password: '',
  };

  error = '';

  constructor(
    private authService: AuthenticationService,
    private router: Router
  ) {}

  login() {
    this.error = '';
    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.router.navigate(['/projects']);
      },
      error: () => {
        this.error = 'Invalid username or password';
      },
    });
  }
}