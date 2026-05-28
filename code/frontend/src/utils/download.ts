import request from "@/utils/request";

export async function downloadFile(url: string, fallbackFilename: string) {
  const res = await request.get(url, {
    responseType: "blob"
  });

  const blob = res instanceof Blob ? res : new Blob([res.data ?? res]);

  // Try to extract filename from Content-Disposition header
  let filename = fallbackFilename;
  try {
    const disposition = (res as any)?.headers?.["content-disposition"];
    if (disposition) {
      const utf8Match = disposition.match(/filename\*=(?:UTF-8''|utf-8'')(.+)/i);
      if (utf8Match) {
        filename = decodeURIComponent(utf8Match[1]);
      } else {
        const asciiMatch = disposition.match(/filename[^;=\n]*=(["']?)(.+?)\1(?:;|$)/);
        if (asciiMatch) {
          filename = asciiMatch[2].replace(/["']/g, "");
        }
      }
    }
  } catch {
    // Use fallback filename
  }

  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
