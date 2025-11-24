import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  get<T>(endpoint: string, params?: any): Observable<T> {
    return this.http.get<T>(`${this.apiUrl}${endpoint}`, { params });
  }

  post<T>(
    endpoint: string,
    body: any,
    options?: { headers?: HttpHeaders; params?: HttpParams }
  ): Observable<T> {
    return this.http.post<T>(`${this.apiUrl}${endpoint}`, body, options);
  }

  put<T>(
    endpoint: string,
    body: any,
    options?: { headers?: HttpHeaders; params?: HttpParams }
  ): Observable<T> {
    return this.http.put<T>(`${this.apiUrl}${endpoint}`, body, options);
  }

  delete<T>(
    endpoint: string,
    options?: { headers?: HttpHeaders; params?: HttpParams }
  ): Observable<T> {
    return this.http.delete<T>(`${this.apiUrl}${endpoint}`, options);
  }
}