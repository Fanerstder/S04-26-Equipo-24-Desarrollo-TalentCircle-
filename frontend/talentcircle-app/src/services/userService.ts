import axios from "axios";
import { Users } from "../types/Users";


const API_URL = "http://localhost:8080/api/users"; 

export const getUsers = async (): Promise<Users[]> => {
  const response = await axios.get<Users[]>(API_URL);
  return response.data;
};

export const getUserById = async (id: number): Promise<Users> => {
  const response = await axios.get<Users>(`${API_URL}/${id}`);
  return response.data;
};

export const createUser = async (user: Users): Promise<Users> => {
  const response = await axios.post<Users>(API_URL, user);
  return response.data;
};
