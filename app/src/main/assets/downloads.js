(() => {
  if (window.fitPhotoSave) return;
  let busy = false;
  window.fitPhotoSave = async (url, name) => {
    if (busy) return;
    if (!window.NativeSave) {
      alert('파일 저장을 사용하려면 Android System WebView와 Chrome을 업데이트해 주세요.');
      return;
    }
    busy = true;
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('fetch failed');
      const blob = await response.blob();
      if (blob.size > 24 * 1024 * 1024) {
        alert('24MB 이하 파일만 앱에서 저장할 수 있습니다. 크기를 줄여 다시 내보내 주세요.');
        return;
      }
      const base64 = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.split(',')[1]);
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      });
      window.NativeSave.postMessage(JSON.stringify({ name: name || 'photo', mime: blob.type || 'image/jpeg', base64 }));
    } catch (error) {
      alert('파일을 준비하지 못했습니다. 다시 내려받아 주세요.');
    } finally { busy = false; }
  };
  document.addEventListener('click', event => {
    const anchor = event.target.closest && event.target.closest('a[download]');
    if (anchor && (anchor.href.startsWith('blob:') || anchor.href.startsWith('data:'))) {
      event.preventDefault();
      window.fitPhotoSave(anchor.href, anchor.download);
    }
  }, true);
})();
