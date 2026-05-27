import request from "@/utils/request";

export async function downloadFile(url: string, filename: string) {
  const res = await request.get(url, { responseType: "blob" });
  const blob = res instanceof Blob ? res : new Blob([res.data ?? res]);
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
