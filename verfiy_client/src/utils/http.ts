import axios from 'axios'

const http = axios.create({
  baseURL: '/verfiy',
  withCredentials: true,
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    return Promise.reject(err)
  }
)

export default http










