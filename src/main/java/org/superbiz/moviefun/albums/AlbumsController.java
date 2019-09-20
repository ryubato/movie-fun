package org.superbiz.moviefun.albums;

import org.apache.tika.io.IOUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.superbiz.moviefun.blobstore.Blob;
import org.superbiz.moviefun.blobstore.BlobStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;

@Controller
@RequestMapping("/albums")
public class AlbumsController {

    private final AlbumsBean albumsBean;
    private final BlobStore blobStore;

    public AlbumsController(AlbumsBean albumsBean, BlobStore blobStore) {
        this.albumsBean = albumsBean;
        this.blobStore = blobStore;
    }

    @GetMapping
    public String index(Map<String, Object> model) {
        model.put("albums", albumsBean.getAlbums());
        return "albums";
    }

    @GetMapping("/{albumId}")
    public String details(@PathVariable long albumId, Map<String, Object> model) {
        model.put("album", albumsBean.find(albumId));
        return "albumDetails";
    }

    /* BlobStore를 사용하여 이미지 저장 */
    @PostMapping("/{albumId}/cover")
    public String uploadCover(@PathVariable long albumId, @RequestParam("file") MultipartFile uploadedFile) throws IOException {

        Blob blob = new Blob(
                getCoverName(albumId),
                uploadedFile.getInputStream(),
                uploadedFile.getContentType()
        );

        blobStore.put(blob);

        return format("redirect:/albums/%d", albumId);
    }

    /* BlobStore를 사용하여 이미지 저장 */
    @GetMapping("/{albumId}/cover")
    public HttpEntity<byte[]> getCover(@PathVariable long albumId) throws IOException, URISyntaxException {
        Optional<Blob> maybeBlob = blobStore.get(getCoverName(albumId));
        Blob blob = maybeBlob.orElseGet(this::defaultCover);  // null 이면 defaultCover() 실행,

        byte[] imageBytes = IOUtils.toByteArray(blob.inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(blob.contentType));
        headers.setContentLength(imageBytes.length);

        return new HttpEntity<>(imageBytes, headers);
    }

    @DeleteMapping("/covers")
    public String deletesCover() {
        blobStore.deleteAll();
        return "redirect:/albums";
    }

    private Blob defaultCover() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("default-cover.jpg");

        return new Blob("default", input, IMAGE_JPEG_VALUE);
        // IMAGE_JPEG_VALUE : .jpg 타입을 문자열로 입력해도 되지만 문자열은 오타가 나도 확인하기 어렵기 때문에 되도록 사용하지 않는다.
        // Springframework의 MimeTypeUtils 을 사용
    }

    private String getCoverName(@PathVariable long albumId) {
        return format("covers/%d", albumId);
    }
}
