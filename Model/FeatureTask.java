import java.util.Set;

class FeatureTask extends Task {
    private float estimatedTime;

    public FeatureTask(String title, Set<String> tags, long id, float estimatedTime) {
        super(title, tags, id);
        this.estimatedTime = estimatedTime;
    }

    public float getEstimatedTime() {
        return estimatedTime;
    }
}
