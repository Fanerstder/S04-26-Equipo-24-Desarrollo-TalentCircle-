export interface Users {
    id: number;
    email: string;
    fullName: string;
    role: "ADMIN" | "EDITOR";
    active: boolean;
    createdAt: string; 
    lastLoginAt?: string;
}